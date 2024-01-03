package org.cyclops.integratedscripting.evaluate.translation;

import com.google.common.collect.Sets;
import net.minecraft.DetectedVersion;
import net.minecraft.SharedConstants;
import net.minecraft.core.Registry;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import org.cyclops.integrateddynamics.api.evaluate.EvaluationException;
import org.cyclops.integrateddynamics.api.evaluate.operator.IOperator;
import org.cyclops.integrateddynamics.api.evaluate.variable.IValue;
import org.cyclops.integrateddynamics.api.evaluate.variable.IValueType;
import org.cyclops.integrateddynamics.api.evaluate.variable.IValueTypeListProxy;
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
        SharedConstants.setVersion(DetectedVersion.BUILT_IN);
        Bootstrap.bootStrap();
        Registry.ITEM.unfreeze();
    }

    private static Context CTX = null;
    private static IEvaluationExceptionFactory EF = ScriptHelpers.getDummyEvaluationExceptionFactory();

    @BeforeClass
    public static void beforeAll() {
        ValueTypeListProxyFactories.load();
        Operators.load();
        ValueTranslators.load();

        try {
            CTX = ScriptHelpers.createPopulatedContext(null);
        } catch (EvaluationException e) {
            e.printStackTrace();
        }
    }

    public static Value getJsValue(String jsString) {
        return CTX.eval("js", jsString);
    }

    @Test(expected = EvaluationException.class)
    public void testUnknownValueToGraal() throws EvaluationException {
        ValueTranslators.REGISTRY.translateToGraal(CTX, DummyValueType.DummyValue.of(), EF);
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
        assertThat(ValueTranslators.REGISTRY.translateFromGraal(CTX, getJsValue("10"), EF), equalTo(ValueTypeInteger.ValueInteger.of(10)));
        assertThat(ValueTranslators.REGISTRY.translateFromGraal(CTX, getJsValue("1.0"), EF), equalTo(ValueTypeInteger.ValueInteger.of(1)));
        assertThat(ValueTranslators.REGISTRY.translateToGraal(CTX, ValueTypeInteger.ValueInteger.of(10), EF), equalTo(getJsValue("10")));
    }

    @Test
    public void testBoolean() throws EvaluationException {
        assertThat(ValueTranslators.REGISTRY.translateFromGraal(CTX, getJsValue("true"), EF), equalTo(ValueTypeBoolean.ValueBoolean.of(true)));
        assertThat(ValueTranslators.REGISTRY.translateToGraal(CTX, ValueTypeBoolean.ValueBoolean.of(true), EF), equalTo(getJsValue("true")));
    }

    @Test
    public void testDouble() throws EvaluationException {
        assertThat(ValueTranslators.REGISTRY.translateFromGraal(CTX, getJsValue("1.1"), EF), equalTo(ValueTypeDouble.ValueDouble.of(1.1D)));

        assertThat(ValueTranslators.REGISTRY.translateToGraal(CTX, ValueTypeDouble.ValueDouble.of(1.1D), EF).asDouble(), equalTo(getJsValue("1.1").asDouble()));
        assertThat(ValueTranslators.REGISTRY.translateToGraal(CTX, ValueTypeDouble.ValueDouble.of(1.0D), EF).asDouble(), equalTo(getJsValue("1.0").asDouble()));
    }

    @Test
    public void testLong() throws EvaluationException {
        assertThat(ValueTranslators.REGISTRY.translateFromGraal(CTX, getJsValue("2147483648"), EF), equalTo(ValueTypeLong.ValueLong.of(2147483648L)));
        assertThat(ValueTranslators.REGISTRY.translateFromGraal(CTX, getJsValue("BigInt('2147483648')"), EF), equalTo(ValueTypeLong.ValueLong.of(2147483648L)));

        assertThat(ValueTranslators.REGISTRY.translateToGraal(CTX, ValueTypeLong.ValueLong.of(2147483648L), EF).asLong(), equalTo(getJsValue("2147483648").asLong()));
    }

    @Test
    public void testString() throws EvaluationException {
        assertThat(ValueTranslators.REGISTRY.translateFromGraal(CTX, getJsValue("'abc'"), EF), equalTo(ValueTypeString.ValueString.of("abc")));
        assertThat(ValueTranslators.REGISTRY.translateToGraal(CTX, ValueTypeString.ValueString.of("abc"), EF).asString(), equalTo(getJsValue("'abc'").asString()));
    }

    @Test
    public void testList() throws EvaluationException {
        assertThat(ValueTranslators.REGISTRY.translateFromGraal(CTX, getJsValue("['abc', 'def', 'ghi']"), EF), equalTo(ValueTypeList.ValueList.ofAll(
                ValueTypeString.ValueString.of("abc"),
                ValueTypeString.ValueString.of("def"),
                ValueTypeString.ValueString.of("ghi")
        )));
        assertThat(ValueTranslators.REGISTRY.translateFromGraal(CTX, getJsValue("['abc', 123, 'ghi']"), EF), equalTo(ValueTypeList.ValueList.ofAll(
                ValueTypeString.ValueString.of("abc"),
                ValueTypeInteger.ValueInteger.of(123),
                ValueTypeString.ValueString.of("ghi")
        )));

        Value translated = ValueTranslators.REGISTRY.translateToGraal(CTX, ValueTypeList.ValueList.ofAll(
                ValueTypeString.ValueString.of("abc"),
                ValueTypeString.ValueString.of("def"),
                ValueTypeString.ValueString.of("ghi")
        ), EF);
        assertThat(translated.hasArrayElements(), is(true));
        assertThat(translated.getArrayElement(0).asString(), equalTo(getJsValue("'abc'").asString()));
        assertThat(translated.getArrayElement(1).asString(), equalTo(getJsValue("'def'").asString()));
        assertThat(translated.getArrayElement(2).asString(), equalTo(getJsValue("'ghi'").asString()));

        Value translatedMixed = ValueTranslators.REGISTRY.translateToGraal(CTX, ValueTypeList.ValueList.ofAll(
                ValueTypeString.ValueString.of("abc"),
                ValueTypeInteger.ValueInteger.of(123),
                ValueTypeString.ValueString.of("ghi")
        ), EF);
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
        }), EF);
    }

    @Test
    public void testOperator() throws EvaluationException {
        IValue operatorValueJs = ValueTranslators.REGISTRY.translateFromGraal(CTX, getJsValue("() => true"), EF);
        assertThat(operatorValueJs.getType(), equalTo(ValueTypes.OPERATOR));

        Value operatorValueJava = ValueTranslators.REGISTRY.translateToGraal(CTX, ValueTypeOperator.ValueOperator.of(Operators.ARITHMETIC_ADDITION), EF);
        assertThat(operatorValueJava.canExecute(), equalTo(true));
    }

    @Test
    public void testOperatorExecuteJavaInJs() throws EvaluationException, IOException {
        Source source = Source.newBuilder("js", "function myFunc(arg0, arg1, arg2) { return arg0(arg1, arg2); }", "src.js").build();
        CTX.eval(source);
        Value myFunc = CTX.getBindings("js").getMember("myFunc");
        Value myFuncRet = myFunc.execute(
                ValueTranslators.REGISTRY.translateToGraal(CTX, ValueTypeOperator.ValueOperator.of(Operators.ARITHMETIC_ADDITION), EF),
                ValueTranslators.REGISTRY.translateToGraal(CTX, ValueTypeInteger.ValueInteger.of(1), EF),
                ValueTranslators.REGISTRY.translateToGraal(CTX, ValueTypeInteger.ValueInteger.of(2), EF)
        );
        assertThat(myFuncRet, equalTo(CTX.asValue(3)));
    }

    @Test
    public void testOperatorExecuteJsInJava() throws EvaluationException {
        IValue operatorValueJs = ValueTranslators.REGISTRY.translateFromGraal(CTX, getJsValue("(arg0, arg1) => arg0 + arg1"), EF);
        IOperator operator = ((ValueTypeOperator.ValueOperator) operatorValueJs).getRawValue();
        IValue value = operator.evaluate(
                new Variable(ValueTypeInteger.ValueInteger.of(1)),
                new Variable(ValueTypeInteger.ValueInteger.of(2))
        );
        assertThat(value, equalTo(ValueTypeInteger.ValueInteger.of(3)));
    }

    @Test
    public void testNbtEnd() throws EvaluationException {
        assertThat(ValueTranslators.REGISTRY.translateFromGraal(CTX, getJsValue("exports = { 'nbt_end': true }"), EF), equalTo(ValueTypeNbt.ValueNbt.of(EndTag.INSTANCE)));

        Value translated = ValueTranslators.REGISTRY.translateToGraal(CTX, ValueTypeNbt.ValueNbt.of(EndTag.INSTANCE), EF);
        assertThat(translated.hasMembers(), is(true));
        assertThat(translated.getMemberKeys(), equalTo(Sets.newHashSet("nbt_end")));
        assertThat(translated.getMember("nbt_end"), equalTo(CTX.asValue(true)));
    }

    @Test
    public void testNbtByte() throws EvaluationException {
        assertThat(ValueTranslators.REGISTRY.translateToGraal(CTX, ValueTypeNbt.ValueNbt.of(ByteTag.valueOf((byte)2)), EF).asByte(), equalTo(getJsValue("2").asByte()));
    }

    @Test
    public void testNbtShort() throws EvaluationException {
        assertThat(ValueTranslators.REGISTRY.translateToGraal(CTX, ValueTypeNbt.ValueNbt.of(ShortTag.valueOf((short) 2)), EF).asShort(), equalTo(getJsValue("2").asShort()));
    }

    @Test
    public void testNbtInt() throws EvaluationException {
        assertThat(ValueTranslators.REGISTRY.translateToGraal(CTX, ValueTypeNbt.ValueNbt.of(IntTag.valueOf(2)), EF).asInt(), equalTo(getJsValue("2").asInt()));
    }

    @Test
    public void testNbtLong() throws EvaluationException {
        assertThat(ValueTranslators.REGISTRY.translateToGraal(CTX, ValueTypeNbt.ValueNbt.of(LongTag.valueOf(2)), EF).asLong(), equalTo(getJsValue("2").asLong()));
    }

    @Test
    public void testNbtFloat() throws EvaluationException {
        assertThat(ValueTranslators.REGISTRY.translateToGraal(CTX, ValueTypeNbt.ValueNbt.of(FloatTag.valueOf(2f)), EF).asFloat(), equalTo(getJsValue("2").asFloat()));
    }

    @Test
    public void testNbtDouble() throws EvaluationException {
        assertThat(ValueTranslators.REGISTRY.translateToGraal(CTX, ValueTypeNbt.ValueNbt.of(DoubleTag.valueOf(2.2)), EF).asDouble(), equalTo(getJsValue("2.2").asDouble()));
    }

    @Test
    public void testNbtByteArray() throws EvaluationException {
        Value list = ValueTranslators.REGISTRY.translateToGraal(CTX, ValueTypeNbt.ValueNbt.of(new ByteArrayTag(new byte[]{1, 2, 3})), EF);

        assertThat(list.hasArrayElements(), is(true));
        assertThat(list.getArrayElement(0).asInt(), equalTo(1));
        assertThat(list.getArrayElement(1).asInt(), equalTo(2));
        assertThat(list.getArrayElement(2).asInt(), equalTo(3));
    }

    @Test
    public void testNbtString() throws EvaluationException {
        assertThat(ValueTranslators.REGISTRY.translateToGraal(CTX, ValueTypeNbt.ValueNbt.of(StringTag.valueOf("abc")), EF).asString(), equalTo(getJsValue("'abc'").asString()));
    }

    @Test
    public void testNbtList() throws EvaluationException {
        ListTag listTag = new ListTag();
        listTag.add(StringTag.valueOf("abc"));
        listTag.add(StringTag.valueOf("def"));
        Value list = ValueTranslators.REGISTRY.translateToGraal(CTX, ValueTypeNbt.ValueNbt.of(listTag), EF);

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
        Value list = ValueTranslators.REGISTRY.translateToGraal(CTX, ValueTypeNbt.ValueNbt.of(listTag), EF);

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
        assertThat(ValueTranslators.REGISTRY.translateFromGraal(CTX, getJsValue("exports = { 'a': 'bla', 'b': { 'c': 123 } }"), EF), equalTo(ValueTypeNbt.ValueNbt.of(compoundTag)));

        Value translated = ValueTranslators.REGISTRY.translateToGraal(CTX, ValueTypeNbt.ValueNbt.of(compoundTag), EF);
        assertThat(translated.hasMembers(), is(true));
        assertThat(translated.getMemberKeys(), equalTo(Sets.newHashSet("a", "b")));
        assertThat(translated.getMember("a").asString(), equalTo(CTX.asValue("bla").asString()));
        Value translatedSub = translated.getMember("b");
        assertThat(translatedSub.getMember("c"), equalTo(CTX.asValue(123)));
    }

    @Test
    public void testNbtIntArray() throws EvaluationException {
        Value list = ValueTranslators.REGISTRY.translateToGraal(CTX, ValueTypeNbt.ValueNbt.of(new IntArrayTag(new int[]{1, 2, 3})), EF);

        assertThat(list.hasArrayElements(), is(true));
        assertThat(list.getArrayElement(0).asInt(), equalTo(1));
        assertThat(list.getArrayElement(1).asInt(), equalTo(2));
        assertThat(list.getArrayElement(2).asInt(), equalTo(3));
    }

    @Test
    public void testNbtLongArray() throws EvaluationException {
        Value list = ValueTranslators.REGISTRY.translateToGraal(CTX, ValueTypeNbt.ValueNbt.of(new LongArrayTag(new long[]{1, 2, 3})), EF);

        assertThat(list.hasArrayElements(), is(true));
        assertThat(list.getArrayElement(0).asLong(), equalTo(1L));
        assertThat(list.getArrayElement(1).asLong(), equalTo(2L));
        assertThat(list.getArrayElement(2).asLong(), equalTo(3L));
    }

    @Test
    public void testObjectBlock() throws EvaluationException {
        assertThat(ValueTranslators.REGISTRY.translateFromGraal(CTX, getJsValue("exports = { id_block: { Name: 'minecraft:stone' } }"), EF), equalTo(ValueObjectTypeBlock.ValueBlock.of(Blocks.STONE.defaultBlockState())));

        Value translated = ValueTranslators.REGISTRY.translateToGraal(CTX, ValueObjectTypeBlock.ValueBlock.of(Blocks.STONE.defaultBlockState()), EF);
        assertThat(translated.hasMembers(), is(true));
        assertThat(translated.getMemberKeys(), equalTo(Sets.newHashSet("id_block")));
        assertThat(translated.getMember("id_block").hasMembers(), is(true));
        assertThat(translated.getMember("id_block").getMemberKeys(), equalTo(Sets.newHashSet("Name")));
        assertThat(translated.getMember("id_block").getMember("Name").asString(), equalTo("minecraft:stone"));
    }

    @Test
    public void testObjectBlockProperties() throws EvaluationException {
        assertThat(ValueTranslators.REGISTRY.translateFromGraal(CTX, getJsValue("exports = { id_block: { Name: 'minecraft:acacia_leaves', Properties: { waterlogged: false, distance: 7, persistent: false } } }"), EF), equalTo(ValueObjectTypeBlock.ValueBlock.of(Blocks.ACACIA_LEAVES.defaultBlockState())));

        Value translated = ValueTranslators.REGISTRY.translateToGraal(CTX, ValueObjectTypeBlock.ValueBlock.of(Blocks.ACACIA_LEAVES.defaultBlockState()), EF);
        assertThat(translated.hasMembers(), is(true));
        assertThat(translated.getMemberKeys(), equalTo(Sets.newHashSet("id_block")));
        assertThat(translated.getMember("id_block").hasMembers(), is(true));
        assertThat(translated.getMember("id_block").getMemberKeys(), equalTo(Sets.newHashSet("Name", "Properties")));
        assertThat(translated.getMember("id_block").getMember("Name").asString(), equalTo("minecraft:acacia_leaves"));
        assertThat(translated.getMember("id_block").getMember("Properties").hasMembers(), is(true));
        assertThat(translated.getMember("id_block").getMember("Properties").getMember("waterlogged").asString(), is("false"));
        assertThat(translated.getMember("id_block").getMember("Properties").getMember("distance").asString(), is("7"));
        assertThat(translated.getMember("id_block").getMember("Properties").getMember("persistent").asString(), is("false"));
    }

    @Test
    public void testObjectBlockUnknown() throws EvaluationException {
        assertThat(ValueTranslators.REGISTRY.translateFromGraal(CTX, getJsValue("exports = { id_block: { Name: 'minecraft:unknown_thing' } }"), EF), equalTo(ValueObjectTypeBlock.ValueBlock.of(Blocks.AIR.defaultBlockState())));
    }

    @Test
    public void testObjectBlockMethods() throws EvaluationException {
        assertThat(ValueTranslators.REGISTRY.translateToGraal(CTX, ValueObjectTypeBlock.ValueBlock.of(Blocks.GLASS.defaultBlockState()), EF).invokeMember("isOpaque").asBoolean(), is(false));
        assertThat(ValueTranslators.REGISTRY.translateToGraal(CTX, ValueObjectTypeBlock.ValueBlock.of(Blocks.STONE.defaultBlockState()), EF).invokeMember("isOpaque").asBoolean(), is(true));
    }

    @Test
    public void testObjectItem() throws EvaluationException {
        assertThat(ValueTranslators.REGISTRY.translateFromGraal(CTX, getJsValue("exports = { id_item: { id: 'minecraft:arrow', Count: 1 } }"), EF), equalTo(ValueObjectTypeItemStack.ValueItemStack.of(new ItemStack(Items.ARROW))));

        Value translated = ValueTranslators.REGISTRY.translateToGraal(CTX, ValueObjectTypeItemStack.ValueItemStack.of(new ItemStack(Items.ARROW)), EF);
        assertThat(translated.hasMembers(), is(true));
        assertThat(translated.getMemberKeys(), equalTo(Sets.newHashSet("id_item")));
        assertThat(translated.getMember("id_item").hasMembers(), is(true));
        assertThat(translated.getMember("id_item").getMemberKeys(), equalTo(Sets.newHashSet("id", "Count")));
        assertThat(translated.getMember("id_item").getMember("id").asString(), equalTo("minecraft:arrow"));
        assertThat(translated.getMember("id_item").getMember("Count").asInt(), equalTo(1));
    }

    @Test
    public void testObjectItemMethods() throws EvaluationException {
        assertThat(ValueTranslators.REGISTRY.translateToGraal(CTX, ValueObjectTypeItemStack.ValueItemStack.of(new ItemStack(Items.ARROW)), EF).invokeMember("canBurn").asBoolean(), is(false));

        assertThat(ValueTranslators.REGISTRY.translateToGraal(CTX, ValueObjectTypeItemStack.ValueItemStack.of(new ItemStack(Items.ACACIA_SAPLING)), EF).invokeMember("block").invokeMember("isPlantable").asBoolean(), is(true));

        assertThat(ValueTranslators.REGISTRY.translateToGraal(CTX, ValueObjectTypeItemStack.ValueItemStack.of(new ItemStack(Items.ACACIA_SAPLING, 1)), EF)
                .invokeMember("equals", ValueTranslators.REGISTRY.translateToGraal(CTX, ValueObjectTypeItemStack.ValueItemStack.of(new ItemStack(Items.ACACIA_SAPLING, 2)), EF)).asBoolean(), is(false));
        assertThat(ValueTranslators.REGISTRY.translateToGraal(CTX, ValueObjectTypeItemStack.ValueItemStack.of(new ItemStack(Items.ACACIA_SAPLING, 1)), EF)
                .invokeMember("equals", ValueTranslators.REGISTRY.translateToGraal(CTX, ValueObjectTypeItemStack.ValueItemStack.of(new ItemStack(Items.ACACIA_SAPLING, 1)), EF)).asBoolean(), is(true));
    }

    @Test
    public void testObjectFluid() throws EvaluationException {
        assertThat(ValueTranslators.REGISTRY.translateFromGraal(CTX, getJsValue("exports = { id_fluid: { FluidName: 'minecraft:water', Amount: 1000 } }"), EF), equalTo(ValueObjectTypeFluidStack.ValueFluidStack.of(new FluidStack(Fluids.WATER, 1000))));

        Value translated = ValueTranslators.REGISTRY.translateToGraal(CTX, ValueObjectTypeFluidStack.ValueFluidStack.of(new FluidStack(Fluids.WATER, 1000)), EF);
        assertThat(translated.hasMembers(), is(true));
        assertThat(translated.getMemberKeys(), equalTo(Sets.newHashSet("id_fluid")));
        assertThat(translated.getMember("id_fluid").hasMembers(), is(true));
        assertThat(translated.getMember("id_fluid").getMemberKeys(), equalTo(Sets.newHashSet("FluidName", "Amount")));
        assertThat(translated.getMember("id_fluid").getMember("FluidName").asString(), equalTo("minecraft:water"));
        assertThat(translated.getMember("id_fluid").getMember("Amount").asInt(), equalTo(1000));
    }

    @Test
    public void testObjectFluidMethods() throws EvaluationException {
        assertThat(ValueTranslators.REGISTRY.translateToGraal(CTX, ValueObjectTypeFluidStack.ValueFluidStack.of(new FluidStack(Fluids.WATER, 1000)), EF).invokeMember("amount").asInt(), is(1000));
        assertThat(ValueTranslators.REGISTRY.translateToGraal(CTX, ValueObjectTypeFluidStack.ValueFluidStack.of(new FluidStack(Fluids.WATER, 123)), EF).invokeMember("amount").asInt(), is(123));
    }

    // Entity, ingredients, and recipe are not easily testable

    @Test
    public void testGlobalFunctions() throws EvaluationException {
        Value ops = CTX.getBindings("js").getMember("idContext").getMember("ops");

        assertThat(
                ops.invokeMember("itemstackSize", ValueTranslators.REGISTRY.translateToGraal(CTX, ValueObjectTypeItemStack.ValueItemStack.of(new ItemStack(Items.ARROW, 10)), EF)).asInt(),
                equalTo(10)
        );

        assertThat(
                ops.invokeMember("anyEquals",
                        ValueTranslators.REGISTRY.translateToGraal(CTX, ValueObjectTypeItemStack.ValueItemStack.of(new ItemStack(Items.ARROW, 10)), EF),
                        ValueTranslators.REGISTRY.translateToGraal(CTX, ValueObjectTypeItemStack.ValueItemStack.of(new ItemStack(Items.ARROW, 20)), EF)
                ).asBoolean(),
                equalTo(false)
        );

        assertThat(
                ops.invokeMember("fluidstackAmount", ValueTranslators.REGISTRY.translateToGraal(CTX, ValueObjectTypeFluidStack.ValueFluidStack.of(new FluidStack(Fluids.WATER, 1000)), EF)).asInt(),
                equalTo(1000)
        );
    }

}
