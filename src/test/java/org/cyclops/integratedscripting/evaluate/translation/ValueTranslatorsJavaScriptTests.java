package org.cyclops.integratedscripting.evaluate.translation;

import com.google.common.collect.Sets;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import org.cyclops.integrateddynamics.api.evaluate.EvaluationException;
import org.cyclops.integrateddynamics.api.evaluate.operator.IOperator;
import org.cyclops.integrateddynamics.api.evaluate.variable.IValue;
import org.cyclops.integrateddynamics.api.evaluate.variable.IValueType;
import org.cyclops.integrateddynamics.api.evaluate.variable.IValueTypeListProxy;
import org.cyclops.integrateddynamics.api.evaluate.variable.ValueDeseralizationContext;
import org.cyclops.integrateddynamics.core.evaluate.operator.Operators;
import org.cyclops.integrateddynamics.core.evaluate.variable.*;
import org.cyclops.integratedscripting.api.evaluate.translation.IEvaluationExceptionFactory;
import org.cyclops.integratedscripting.evaluate.ScriptHelpers;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.Iterator;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author rubensworks
 */
public class ValueTranslatorsJavaScriptTests {

    static {
        Bootstrap.bootStrap();
    }

    private static ValueDeseralizationContext VDC = null;
    private static Context CTX = null;
    private static IEvaluationExceptionFactory EF = ScriptHelpers.getDummyEvaluationExceptionFactory();

    @BeforeClass
    public static void beforeAll() {
        ValueTypeListProxyFactories.load();
        Operators.load();
        ValueTranslators.load();

        VDC = ValueDeseralizationContextMocked.get();
        try {
            CTX = ScriptHelpers.createPopulatedContext(null, VDC);
        } catch (EvaluationException e) {
            e.printStackTrace();
        }
    }

    public static Value getJsValue(String jsString) {
        return CTX.eval("js", jsString);
    }

    @Test(expected = EvaluationException.class)
    public void testUnknownValueToGraal() throws EvaluationException {
        ValueTranslators.REGISTRY.translateToGraal(CTX, DummyValueType.DummyValue.of(), EF, VDC);
    }

    @Test(expected = EvaluationException.class)
    public void testUnknownValueToGraalNbt() throws EvaluationException {
        ValueTranslators.REGISTRY.translateToNbt(CTX, DummyValueType.DummyValue.of(), EF);
    }

// Untestable
//    @Test(expected = EvaluationException.class)
//    public void testUnknownValueFromGraal() throws EvaluationException {
//        ValueTranslators.REGISTRY.translate(CTX, getJsValue("?"));
//    }

    @Test
    public void testInteger() throws EvaluationException {
        assertThat(ValueTranslators.REGISTRY.translateFromGraal(CTX, getJsValue("10"), EF, VDC), equalTo(ValueTypeInteger.ValueInteger.of(10)));
        assertThat(ValueTranslators.REGISTRY.translateFromGraal(CTX, getJsValue("1.0"), EF, VDC), equalTo(ValueTypeInteger.ValueInteger.of(1)));
        assertThat(ValueTranslators.REGISTRY.translateToGraal(CTX, ValueTypeInteger.ValueInteger.of(10), EF, VDC), equalTo(getJsValue("10")));
    }

    @Test
    public void testBoolean() throws EvaluationException {
        assertThat(ValueTranslators.REGISTRY.translateFromGraal(CTX, getJsValue("true"), EF, VDC), equalTo(ValueTypeBoolean.ValueBoolean.of(true)));
        assertThat(ValueTranslators.REGISTRY.translateToGraal(CTX, ValueTypeBoolean.ValueBoolean.of(true), EF, VDC), equalTo(getJsValue("true")));
    }

    @Test
    public void testDouble() throws EvaluationException {
        assertThat(ValueTranslators.REGISTRY.translateFromGraal(CTX, getJsValue("1.1"), EF, VDC), equalTo(ValueTypeDouble.ValueDouble.of(1.1D)));

        assertThat(ValueTranslators.REGISTRY.translateToGraal(CTX, ValueTypeDouble.ValueDouble.of(1.1D), EF, VDC).asDouble(), equalTo(getJsValue("1.1").asDouble()));
        assertThat(ValueTranslators.REGISTRY.translateToGraal(CTX, ValueTypeDouble.ValueDouble.of(1.0D), EF, VDC).asDouble(), equalTo(getJsValue("1.0").asDouble()));
    }

    @Test
    public void testLong() throws EvaluationException {
        assertThat(ValueTranslators.REGISTRY.translateFromGraal(CTX, getJsValue("2147483648"), EF, VDC), equalTo(ValueTypeLong.ValueLong.of(2147483648L)));
        assertThat(ValueTranslators.REGISTRY.translateFromGraal(CTX, getJsValue("BigInt('2147483648')"), EF, VDC), equalTo(ValueTypeLong.ValueLong.of(2147483648L)));

        assertThat(ValueTranslators.REGISTRY.translateToGraal(CTX, ValueTypeLong.ValueLong.of(2147483648L), EF, VDC).asLong(), equalTo(getJsValue("2147483648").asLong()));
    }

    @Test
    public void testString() throws EvaluationException {
        assertThat(ValueTranslators.REGISTRY.translateFromGraal(CTX, getJsValue("'abc'"), EF, VDC), equalTo(ValueTypeString.ValueString.of("abc")));
        assertThat(ValueTranslators.REGISTRY.translateToGraal(CTX, ValueTypeString.ValueString.of("abc"), EF, VDC).asString(), equalTo(getJsValue("'abc'").asString()));
    }

    @Test
    public void testList() throws EvaluationException {
        assertThat(ValueTranslators.REGISTRY.translateFromGraal(CTX, getJsValue("['abc', 'def', 'ghi']"), EF, VDC), equalTo(ValueTypeList.ValueList.ofAll(
                ValueTypeString.ValueString.of("abc"),
                ValueTypeString.ValueString.of("def"),
                ValueTypeString.ValueString.of("ghi")
        )));
        assertThat(ValueTranslators.REGISTRY.translateFromGraal(CTX, getJsValue("['abc', 123, 'ghi']"), EF, VDC), equalTo(ValueTypeList.ValueList.ofAll(
                ValueTypeString.ValueString.of("abc"),
                ValueTypeInteger.ValueInteger.of(123),
                ValueTypeString.ValueString.of("ghi")
        )));

        Value translated = ValueTranslators.REGISTRY.translateToGraal(CTX, ValueTypeList.ValueList.ofAll(
                ValueTypeString.ValueString.of("abc"),
                ValueTypeString.ValueString.of("def"),
                ValueTypeString.ValueString.of("ghi")
        ), EF, VDC);
        assertThat(translated.hasArrayElements(), is(true));
        assertThat(translated.getArrayElement(0).asString(), equalTo(getJsValue("'abc'").asString()));
        assertThat(translated.getArrayElement(1).asString(), equalTo(getJsValue("'def'").asString()));
        assertThat(translated.getArrayElement(2).asString(), equalTo(getJsValue("'ghi'").asString()));

        Value translatedMixed = ValueTranslators.REGISTRY.translateToGraal(CTX, ValueTypeList.ValueList.ofAll(
                ValueTypeString.ValueString.of("abc"),
                ValueTypeInteger.ValueInteger.of(123),
                ValueTypeString.ValueString.of("ghi")
        ), EF, VDC);
        assertThat(translatedMixed.hasArrayElements(), is(true));
        assertThat(translatedMixed.getArrayElement(0).asString(), equalTo(getJsValue("'abc'").asString()));
        assertThat(translatedMixed.getArrayElement(1).asInt(), equalTo(getJsValue("123").asInt()));
        assertThat(translatedMixed.getArrayElement(2).asString(), equalTo(getJsValue("'ghi'").asString()));
    }

    @Test(expected = EvaluationException.class)
    public void testListInfinity() throws EvaluationException {
        ValueTranslators.REGISTRY.translateToGraal(CTX, ValueTypeList.ValueList.ofFactory(new IValueTypeListProxy() {
            @Override
            public int getLength() throws EvaluationException {
                return 0;
            }

            @Override
            public IValue get(int index) throws EvaluationException {
                return null;
            }

            @Override
            public IValueType<? extends IValue> getValueType() {
                return null;
            }

            @Override
            public ResourceLocation getName() {
                return null;
            }

            @Override
            public MutableComponent toCompactString() {
                return null;
            }

            @Override
            public boolean isInfinite() {
                return true;
            }

            @NotNull
            @Override
            public Iterator<IValue> iterator() {
                return null;
            }
        }), EF, VDC);
    }

    @Test
    public void testOperator() throws EvaluationException {
        IValue operatorValueJs = ValueTranslators.REGISTRY.translateFromGraal(CTX, getJsValue("() => true"), EF, VDC);
        assertThat(operatorValueJs.getType(), equalTo(ValueTypes.OPERATOR));

        Value operatorValueJava = ValueTranslators.REGISTRY.translateToGraal(CTX, ValueTypeOperator.ValueOperator.of(Operators.ARITHMETIC_ADDITION), EF, VDC);
        assertThat(operatorValueJava.canExecute(), equalTo(true));
    }

    @Test
    public void testOperatorBidirectional() throws EvaluationException {
        ValueTypeOperator.ValueOperator operatorValue = ValueTypeOperator.ValueOperator.of(Operators.ARITHMETIC_ADDITION);
        Value operatorValueJava = ValueTranslators.REGISTRY.translateToGraal(CTX, operatorValue, EF, VDC);
        IValue operatorValueJs = ValueTranslators.REGISTRY.translateFromGraal(CTX, operatorValueJava, EF, VDC);

        assertThat(operatorValueJs, equalTo(operatorValue));
        assertThat(operatorValueJava.isProxyObject(), equalTo(true));
    }

    @Test
    public void testOperatorExecuteJavaInJs() throws EvaluationException, IOException {
        Source source = Source.newBuilder("js", "function myFunc(arg0, arg1, arg2) { return arg0(arg1, arg2); }", "src.js").build();
        CTX.eval(source);
        Value myFunc = CTX.getBindings("js").getMember("myFunc");
        Value myFuncRet = myFunc.execute(
                ValueTranslators.REGISTRY.translateToGraal(CTX, ValueTypeOperator.ValueOperator.of(Operators.ARITHMETIC_ADDITION), EF, VDC),
                ValueTranslators.REGISTRY.translateToGraal(CTX, ValueTypeInteger.ValueInteger.of(1), EF, VDC),
                ValueTranslators.REGISTRY.translateToGraal(CTX, ValueTypeInteger.ValueInteger.of(2), EF, VDC)
        );
        assertThat(myFuncRet, equalTo(CTX.asValue(3)));
    }

    @Test
    public void testOperatorExecuteJsInJava() throws EvaluationException {
        IValue operatorValueJs = ValueTranslators.REGISTRY.translateFromGraal(CTX, getJsValue("(arg0, arg1) => arg0 + arg1"), EF, VDC);
        IOperator operator = ((ValueTypeOperator.ValueOperator) operatorValueJs).getRawValue();
        IValue value = operator.evaluate(
                new Variable(ValueTypeInteger.ValueInteger.of(1)),
                new Variable(ValueTypeInteger.ValueInteger.of(2))
        );
        assertThat(value, equalTo(ValueTypeInteger.ValueInteger.of(3)));
    }

    @Test
    public void testNbtEnd() throws EvaluationException {
        assertThat(ValueTranslators.REGISTRY.translateFromGraal(CTX, getJsValue("exports = { 'nbt_end': true }"), EF, VDC), equalTo(ValueTypeNbt.ValueNbt.of(EndTag.INSTANCE)));

        Value translated = ValueTranslators.REGISTRY.translateToGraal(CTX, ValueTypeNbt.ValueNbt.of(EndTag.INSTANCE), EF, VDC);
        assertThat(translated.hasMembers(), is(true));
        assertThat(translated.getMemberKeys(), equalTo(Sets.newHashSet("nbt_end")));
        assertThat(translated.getMember("nbt_end"), equalTo(CTX.asValue(true)));
    }

    @Test
    public void testNbtByte() throws EvaluationException {
        assertThat(ValueTranslators.REGISTRY.translateToGraal(CTX, ValueTypeNbt.ValueNbt.of(ByteTag.valueOf((byte)2)), EF, VDC).asByte(), equalTo(getJsValue("2").asByte()));
    }

    @Test
    public void testNbtShort() throws EvaluationException {
        assertThat(ValueTranslators.REGISTRY.translateToGraal(CTX, ValueTypeNbt.ValueNbt.of(ShortTag.valueOf((short) 2)), EF, VDC).asShort(), equalTo(getJsValue("2").asShort()));
    }

    @Test
    public void testNbtInt() throws EvaluationException {
        assertThat(ValueTranslators.REGISTRY.translateToGraal(CTX, ValueTypeNbt.ValueNbt.of(IntTag.valueOf(2)), EF, VDC).asInt(), equalTo(getJsValue("2").asInt()));
    }

    @Test
    public void testNbtLong() throws EvaluationException {
        assertThat(ValueTranslators.REGISTRY.translateToGraal(CTX, ValueTypeNbt.ValueNbt.of(LongTag.valueOf(2)), EF, VDC).asLong(), equalTo(getJsValue("2").asLong()));
    }

    @Test
    public void testNbtFloat() throws EvaluationException {
        assertThat(ValueTranslators.REGISTRY.translateToGraal(CTX, ValueTypeNbt.ValueNbt.of(FloatTag.valueOf(2f)), EF, VDC).asFloat(), equalTo(getJsValue("2").asFloat()));
    }

    @Test
    public void testNbtDouble() throws EvaluationException {
        assertThat(ValueTranslators.REGISTRY.translateToGraal(CTX, ValueTypeNbt.ValueNbt.of(DoubleTag.valueOf(2.2)), EF, VDC).asDouble(), equalTo(getJsValue("2.2").asDouble()));
    }

    @Test
    public void testNbtByteArray() throws EvaluationException {
        Value list = ValueTranslators.REGISTRY.translateToGraal(CTX, ValueTypeNbt.ValueNbt.of(new ByteArrayTag(new byte[]{1, 2, 3})), EF, VDC);

        assertThat(list.hasArrayElements(), is(true));
        assertThat(list.getArrayElement(0).asInt(), equalTo(1));
        assertThat(list.getArrayElement(1).asInt(), equalTo(2));
        assertThat(list.getArrayElement(2).asInt(), equalTo(3));
    }

    @Test
    public void testNbtString() throws EvaluationException {
        assertThat(ValueTranslators.REGISTRY.translateToGraal(CTX, ValueTypeNbt.ValueNbt.of(StringTag.valueOf("abc")), EF, VDC).asString(), equalTo(getJsValue("'abc'").asString()));
    }

    @Test
    public void testNbtList() throws EvaluationException {
        ListTag listTag = new ListTag();
        listTag.add(StringTag.valueOf("abc"));
        listTag.add(StringTag.valueOf("def"));
        Value list = ValueTranslators.REGISTRY.translateToGraal(CTX, ValueTypeNbt.ValueNbt.of(listTag), EF, VDC);

        assertThat(list.hasArrayElements(), is(true));
        assertThat(list.getArrayElement(0).asString(), equalTo("abc"));
        assertThat(list.getArrayElement(1).asString(), equalTo("def"));
    }

    @Test
    public void testNbtListNested() throws EvaluationException {
        ListTag listTag = new ListTag();
        ListTag listTagInner1 = new ListTag();
        listTagInner1.add(StringTag.valueOf("abc"));
        listTag.add(listTagInner1);
        ListTag listTagInner2 = new ListTag();
        listTagInner2.add(StringTag.valueOf("def"));
        listTag.add(listTagInner2);
        Value list = ValueTranslators.REGISTRY.translateToGraal(CTX, ValueTypeNbt.ValueNbt.of(listTag), EF, VDC);

        assertThat(list.hasArrayElements(), is(true));
        assertThat(list.getArrayElement(0).hasArrayElements(), equalTo(true));
        assertThat(list.getArrayElement(0).getArrayElement(0).asString(), equalTo("abc"));
        assertThat(list.getArrayElement(1).hasArrayElements(), equalTo(true));
        assertThat(list.getArrayElement(1).getArrayElement(0).asString(), equalTo("def"));
    }

    @Test
    public void testNbtCompound() throws EvaluationException {
        CompoundTag compoundTag = new CompoundTag();
        CompoundTag compoundTagSub = new CompoundTag();
        compoundTag.put("a", StringTag.valueOf("bla"));
        compoundTag.put("b", compoundTagSub);
        compoundTagSub.put("c", IntTag.valueOf(123));
        compoundTagSub.put("d", LongTag.valueOf(2147483648L));
        compoundTagSub.put("e", DoubleTag.valueOf(2.2D));
        ListTag listTag = new ListTag();
        listTag.add(IntTag.valueOf(1));
        listTag.add(IntTag.valueOf(2));
        listTag.add(IntTag.valueOf(3));
        compoundTagSub.put("f", listTag);
        assertThat(ValueTranslators.REGISTRY.translateFromGraal(CTX, getJsValue("exports = { 'a': 'bla', 'b': { 'c': 123, 'd': 2147483648, 'e': 2.2, 'f': [1, 2, 3] } }"), EF, VDC), equalTo(ValueTypeNbt.ValueNbt.of(compoundTag)));

        Value translated = ValueTranslators.REGISTRY.translateToGraal(CTX, ValueTypeNbt.ValueNbt.of(compoundTag), EF, VDC);
        assertThat(translated.hasMembers(), is(true));
        assertThat(translated.getMemberKeys(), equalTo(Sets.newHashSet("a", "b")));
        assertThat(translated.getMember("a").asString(), equalTo(CTX.asValue("bla").asString()));
        Value translatedSub = translated.getMember("b");
        assertThat(translatedSub.getMember("c"), equalTo(CTX.asValue(123)));
        assertThat(translatedSub.getMember("d").asLong(), equalTo(CTX.asValue(2147483648L).asLong()));
        assertThat(translatedSub.getMember("e").asDouble(), equalTo(CTX.asValue(2.2D).asDouble()));
        Value listValue = translatedSub.getMember("f");
        assertThat(listValue.hasArrayElements(), equalTo(true));
        assertThat(listValue.getArrayElement(0), equalTo(CTX.asValue(1)));
        assertThat(listValue.getArrayElement(1), equalTo(CTX.asValue(2)));
        assertThat(listValue.getArrayElement(2), equalTo(CTX.asValue(3)));
    }

    @Test
    public void testNbtCompoundBidirectional() throws EvaluationException {
        CompoundTag compoundTag = new CompoundTag();
        CompoundTag compoundTagSub = new CompoundTag();
        compoundTag.put("a", StringTag.valueOf("bla"));
        compoundTag.put("b", compoundTagSub);
        compoundTagSub.put("c", IntTag.valueOf(123));

        Value translated1 = ValueTranslators.REGISTRY.translateToGraal(CTX, ValueTypeNbt.ValueNbt.of(compoundTag), EF, VDC);
        IValue translated2 = ValueTranslators.REGISTRY.translateFromGraal(CTX, translated1, EF, VDC);

        assertThat(translated2, equalTo(ValueTypeNbt.ValueNbt.of(compoundTag)));
        assertThat(translated1.isProxyObject(), equalTo(true));
    }

    @Test
    public void testNbtCompoundModifyInJs() throws EvaluationException {
        CompoundTag compoundTag = new CompoundTag();
        CompoundTag compoundTagSub = new CompoundTag();
        compoundTag.put("a", StringTag.valueOf("bla"));
        compoundTag.put("b", compoundTagSub);
        compoundTagSub.put("c", IntTag.valueOf(123));

        Value translated1 = ValueTranslators.REGISTRY.translateToGraal(CTX, ValueTypeNbt.ValueNbt.of(compoundTag), EF, VDC);
        translated1.putMember("c", CTX.asValue(456));

        assertThat(compoundTag.getInt("c"), equalTo(456));
    }

    @Test
    public void testNbtIntArray() throws EvaluationException {
        Value list = ValueTranslators.REGISTRY.translateToGraal(CTX, ValueTypeNbt.ValueNbt.of(new IntArrayTag(new int[]{1, 2, 3})), EF, VDC);

        assertThat(list.hasArrayElements(), is(true));
        assertThat(list.getArrayElement(0).asInt(), equalTo(1));
        assertThat(list.getArrayElement(1).asInt(), equalTo(2));
        assertThat(list.getArrayElement(2).asInt(), equalTo(3));
    }

    @Test
    public void testNbtLongArray() throws EvaluationException {
        Value list = ValueTranslators.REGISTRY.translateToGraal(CTX, ValueTypeNbt.ValueNbt.of(new LongArrayTag(new long[]{1, 2, 3})), EF, VDC);

        assertThat(list.hasArrayElements(), is(true));
        assertThat(list.getArrayElement(0).asLong(), equalTo(1L));
        assertThat(list.getArrayElement(1).asLong(), equalTo(2L));
        assertThat(list.getArrayElement(2).asLong(), equalTo(3L));
    }

    // Disabled due to unavailable registry
//    @Test
//    public void testObjectBlock() throws EvaluationException {
//        assertThat(ValueTranslators.REGISTRY.translateFromGraal(CTX, getJsValue("exports = { id_block: { Name: 'minecraft:stone' } }"), EF, VDC), equalTo(ValueObjectTypeBlock.ValueBlock.of(Blocks.STONE.defaultBlockState())));
//
//        Value translated = ValueTranslators.REGISTRY.translateToGraal(CTX, ValueObjectTypeBlock.ValueBlock.of(Blocks.STONE.defaultBlockState()), EF, VDC);
//        assertThat(translated.hasMembers(), is(true));
//        assertThat(translated.getMemberKeys(), equalTo(Sets.newHashSet("id_block")));
//        assertThat(translated.getMember("id_block").hasMembers(), is(true));
//        assertThat(translated.getMember("id_block").getMemberKeys(), equalTo(Sets.newHashSet("Name")));
//        assertThat(translated.getMember("id_block").getMember("Name").asString(), equalTo("minecraft:stone"));
//    }
//
//    @Test
//    public void testObjectBlockProperties() throws EvaluationException {
//        assertThat(ValueTranslators.REGISTRY.translateFromGraal(CTX, getJsValue("exports = { id_block: { Name: 'minecraft:acacia_leaves', Properties: { waterlogged: false, distance: 7, persistent: false } } }"), EF, VDC), equalTo(ValueObjectTypeBlock.ValueBlock.of(Blocks.ACACIA_LEAVES.defaultBlockState())));
//
//        Value translated = ValueTranslators.REGISTRY.translateToGraal(CTX, ValueObjectTypeBlock.ValueBlock.of(Blocks.ACACIA_LEAVES.defaultBlockState()), EF, VDC);
//        assertThat(translated.hasMembers(), is(true));
//        assertThat(translated.getMemberKeys(), equalTo(Sets.newHashSet("id_block")));
//        assertThat(translated.getMember("id_block").hasMembers(), is(true));
//        assertThat(translated.getMember("id_block").getMemberKeys(), equalTo(Sets.newHashSet("Name", "Properties")));
//        assertThat(translated.getMember("id_block").getMember("Name").asString(), equalTo("minecraft:acacia_leaves"));
//        assertThat(translated.getMember("id_block").getMember("Properties").hasMembers(), is(true));
//        assertThat(translated.getMember("id_block").getMember("Properties").getMember("waterlogged").asString(), is("false"));
//        assertThat(translated.getMember("id_block").getMember("Properties").getMember("distance").asString(), is("7"));
//        assertThat(translated.getMember("id_block").getMember("Properties").getMember("persistent").asString(), is("false"));
//    }
//
//    @Test
//    public void testObjectBlockUnknown() throws EvaluationException {
//        assertThat(ValueTranslators.REGISTRY.translateFromGraal(CTX, getJsValue("exports = { id_block: { Name: 'minecraft:unknown_thing' } }"), EF, VDC), equalTo(ValueObjectTypeBlock.ValueBlock.of(Blocks.AIR.defaultBlockState())));
//    }

    @Test
    public void testObjectBlockMethods() throws EvaluationException {
        assertThat(ValueTranslators.REGISTRY.translateToGraal(CTX, ValueObjectTypeBlock.ValueBlock.of(Blocks.GLASS.defaultBlockState()), EF, VDC).invokeMember("isOpaque").asBoolean(), is(false));
        assertThat(ValueTranslators.REGISTRY.translateToGraal(CTX, ValueObjectTypeBlock.ValueBlock.of(Blocks.STONE.defaultBlockState()), EF, VDC).invokeMember("isOpaque").asBoolean(), is(true));
    }

    @Test
    public void testObjectItem() throws EvaluationException {
        assertThat(ValueTranslators.REGISTRY.translateFromGraal(CTX, getJsValue("exports = { id_item: { id: 'minecraft:arrow', count: 1 } }"), EF, VDC), equalTo(ValueObjectTypeItemStack.ValueItemStack.of(new ItemStack(Items.ARROW))));

        Value translated = ValueTranslators.REGISTRY.translateToGraal(CTX, ValueObjectTypeItemStack.ValueItemStack.of(new ItemStack(Items.ARROW)), EF, VDC);
        assertThat(translated.hasMembers(), is(true));
        assertThat(translated.getMemberKeys(), equalTo(Sets.newHashSet("id_item")));
        assertThat(translated.getMember("id_item").hasMembers(), is(true));
        assertThat(translated.getMember("id_item").getMemberKeys(), equalTo(Sets.newHashSet("id", "count")));
        assertThat(translated.getMember("id_item").getMember("id").asString(), equalTo("minecraft:arrow"));
        assertThat(translated.getMember("id_item").getMember("count").asInt(), equalTo(1));
    }

    @Test
    public void testObjectItemBidirectional() throws EvaluationException {
        ValueObjectTypeItemStack.ValueItemStack value = ValueObjectTypeItemStack.ValueItemStack.of(new ItemStack(Items.ARROW));
        Value translated1 = ValueTranslators.REGISTRY.translateToGraal(CTX, value, EF, VDC);
        IValue translated2 = ValueTranslators.REGISTRY.translateFromGraal(CTX, translated1, EF, VDC);
        assertThat(translated2, equalTo(value));
        assertThat(translated1.isProxyObject(), equalTo(true));
    }

    @Test
    public void testObjectItemMethods() throws EvaluationException {
        assertThat(ValueTranslators.REGISTRY.translateToGraal(CTX, ValueObjectTypeItemStack.ValueItemStack.of(new ItemStack(Items.ARROW)), EF, VDC).invokeMember("canBurn").asBoolean(), is(false));

        assertThat(ValueTranslators.REGISTRY.translateToGraal(CTX, ValueObjectTypeItemStack.ValueItemStack.of(new ItemStack(Items.ACACIA_SAPLING)), EF, VDC).invokeMember("block").invokeMember("plantAge").asInt(), is(0));

        assertThat(ValueTranslators.REGISTRY.translateToGraal(CTX, ValueObjectTypeItemStack.ValueItemStack.of(new ItemStack(Items.ACACIA_SAPLING, 1)), EF, VDC)
                .invokeMember("equals", ValueTranslators.REGISTRY.translateToGraal(CTX, ValueObjectTypeItemStack.ValueItemStack.of(new ItemStack(Items.ACACIA_SAPLING, 2)), EF, VDC)).asBoolean(), is(false));
        assertThat(ValueTranslators.REGISTRY.translateToGraal(CTX, ValueObjectTypeItemStack.ValueItemStack.of(new ItemStack(Items.ACACIA_SAPLING, 1)), EF, VDC)
                .invokeMember("equals", ValueTranslators.REGISTRY.translateToGraal(CTX, ValueObjectTypeItemStack.ValueItemStack.of(new ItemStack(Items.ACACIA_SAPLING, 1)), EF, VDC)).asBoolean(), is(true));
    }

    @Test
    public void testObjectFluid() throws EvaluationException {
        assertThat(ValueTranslators.REGISTRY.translateFromGraal(CTX, getJsValue("exports = { id_fluid: { id: 'minecraft:water', amount: 1000 } }"), EF, VDC), equalTo(ValueObjectTypeFluidStack.ValueFluidStack.of(new FluidStack(Fluids.WATER, 1000))));

        Value translated = ValueTranslators.REGISTRY.translateToGraal(CTX, ValueObjectTypeFluidStack.ValueFluidStack.of(new FluidStack(Fluids.WATER, 1000)), EF, VDC);
        assertThat(translated.hasMembers(), is(true));
        assertThat(translated.getMemberKeys(), equalTo(Sets.newHashSet("id_fluid")));
        assertThat(translated.getMember("id_fluid").hasMembers(), is(true));
        assertThat(translated.getMember("id_fluid").getMemberKeys(), equalTo(Sets.newHashSet("amount", "id")));
        assertThat(translated.getMember("id_fluid").getMember("id").asString(), equalTo("minecraft:water"));
        assertThat(translated.getMember("id_fluid").getMember("amount").asInt(), equalTo(1000));
    }

    @Test
    public void testObjectFluidMethods() throws EvaluationException {
        assertThat(ValueTranslators.REGISTRY.translateToGraal(CTX, ValueObjectTypeFluidStack.ValueFluidStack.of(new FluidStack(Fluids.WATER, 1000)), EF, VDC).invokeMember("amount").asInt(), is(1000));
        assertThat(ValueTranslators.REGISTRY.translateToGraal(CTX, ValueObjectTypeFluidStack.ValueFluidStack.of(new FluidStack(Fluids.WATER, 123)), EF, VDC).invokeMember("amount").asInt(), is(123));
    }

    // Entity, ingredients, and recipe are not easily testable

    @Test
    public void testGlobalFunctions() throws EvaluationException {
        Value ops = CTX.getBindings("js").getMember("idContext").getMember("ops");

        assertThat(
                ops.invokeMember("itemstackSize", ValueTranslators.REGISTRY.translateToGraal(CTX, ValueObjectTypeItemStack.ValueItemStack.of(new ItemStack(Items.ARROW, 10)), EF, VDC)).asInt(),
                equalTo(10)
        );

        assertThat(
                ops.invokeMember("anyEquals",
                        ValueTranslators.REGISTRY.translateToGraal(CTX, ValueObjectTypeItemStack.ValueItemStack.of(new ItemStack(Items.ARROW, 10)), EF, VDC),
                        ValueTranslators.REGISTRY.translateToGraal(CTX, ValueObjectTypeItemStack.ValueItemStack.of(new ItemStack(Items.ARROW, 20)), EF, VDC)
                ).asBoolean(),
                equalTo(false)
        );

        assertThat(
                ops.invokeMember("fluidstackAmount", ValueTranslators.REGISTRY.translateToGraal(CTX, ValueObjectTypeFluidStack.ValueFluidStack.of(new FluidStack(Fluids.WATER, 1000)), EF, VDC)).asInt(),
                equalTo(1000)
        );
    }

}
