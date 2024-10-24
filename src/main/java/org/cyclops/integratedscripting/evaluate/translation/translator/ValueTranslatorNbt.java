package org.cyclops.integratedscripting.evaluate.translation.translator;

import com.google.common.collect.Sets;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import org.cyclops.integrateddynamics.api.evaluate.EvaluationException;
import org.cyclops.integrateddynamics.api.evaluate.operator.IOperator;
import org.cyclops.integrateddynamics.api.evaluate.variable.IValue;
import org.cyclops.integrateddynamics.api.evaluate.variable.IValueType;
import org.cyclops.integrateddynamics.api.evaluate.variable.ValueDeseralizationContext;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypeNbt;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypes;
import org.cyclops.integratedscripting.api.evaluate.translation.IEvaluationExceptionFactory;
import org.cyclops.integratedscripting.api.evaluate.translation.IValueTranslator;
import org.cyclops.integratedscripting.evaluate.translation.ValueTranslators;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author rubensworks
 */
public class ValueTranslatorNbt implements IValueTranslator<ValueTypeNbt.ValueNbt> {

    @Override
    public IValueType<?> getValueType() {
        return ValueTypes.NBT;
    }

    @Override
    public boolean canHandleGraalValue(Value value) {
        return value.hasMembers();
    }

    @Override
    public boolean canTranslateNbt() {
        return true;
    }

    @Override
    public Value translateToGraal(Context context, ValueTypeNbt.ValueNbt value, IEvaluationExceptionFactory exceptionFactory, ValueDeseralizationContext valueDeseralizationContext) throws EvaluationException {
        Tag tag = value.getRawValue().orElse(null);
        if (tag == null) {
            return context.asValue(null);
        }
        return translateTag(context, tag, exceptionFactory, valueDeseralizationContext);
    }

    public Value translateTag(Context context, Tag tag, IEvaluationExceptionFactory exceptionFactory, ValueDeseralizationContext valueDeseralizationContext) throws EvaluationException {
        switch (tag.getId()) {
            case Tag.TAG_END -> {
                return context.eval("js", "exports = { 'nbt_end': true }");
            }
            case Tag.TAG_BYTE -> {
                return context.asValue(((ByteTag) tag).getAsByte());
            }
            case Tag.TAG_SHORT -> {
                return context.asValue(((ShortTag) tag).getAsShort());
            }
            case Tag.TAG_INT -> {
                return context.asValue(((IntTag) tag).getAsInt());
            }
            case Tag.TAG_LONG -> {
                return context.asValue(((LongTag) tag).getAsLong());
            }
            case Tag.TAG_FLOAT -> {
                return context.asValue(((FloatTag) tag).getAsFloat());
            }
            case Tag.TAG_DOUBLE -> {
                return context.asValue(((DoubleTag) tag).getAsDouble());
            }
            case Tag.TAG_BYTE_ARRAY -> {
                return context.asValue(((ByteArrayTag) tag).getAsByteArray());
            }
            case Tag.TAG_STRING -> {
                return context.asValue(tag.getAsString());
            }
            case Tag.TAG_LIST -> {
                List<Value> list = new ArrayList<>();
                ListTag listTag = (ListTag) tag;
                for (Tag innerValue : listTag) {
                    list.add(translateTag(context, innerValue, exceptionFactory, valueDeseralizationContext));
                }
                return context.asValue(list);
            }
            case Tag.TAG_COMPOUND -> {
                return translateCompoundTag(context, (CompoundTag) tag, exceptionFactory, null, null, valueDeseralizationContext);
            }
            case Tag.TAG_INT_ARRAY -> {
                return context.asValue(((IntArrayTag) tag).getAsIntArray());
            }
            case Tag.TAG_LONG_ARRAY -> {
                return context.asValue(((LongArrayTag) tag).getAsLongArray());
            }
            default -> throw exceptionFactory.createError(Component.translatable("valuetype.integratedscripting.error.translation.nbt_unknown", tag.getType().getPrettyName()));
        }
    }

    public Value translateCompoundTag(Context context, CompoundTag tag, IEvaluationExceptionFactory exceptionFactory, @Nullable Map<String, IOperator> methods, @Nullable IValue value, ValueDeseralizationContext valueDeseralizationContext) {
        return context.asValue(new NbtCompoundTagProxyObject(context, exceptionFactory, tag, methods, value, valueDeseralizationContext));
    }

    @Override
    public ValueTypeNbt.ValueNbt translateFromGraal(Context context, Value value, IEvaluationExceptionFactory exceptionFactory, ValueDeseralizationContext valueDeseralizationContext) throws EvaluationException {
        // Unwrap the value if it was translated in the opposite direction before.
        if (value.isProxyObject()) {
            try {
                NbtCompoundTagProxyObject proxy = value.asProxyObject();
                return ValueTypeNbt.ValueNbt.of(proxy.getTag());
            } catch (ClassCastException classCastException) {
                // Fallback to case below
            }
        }

        if (value.getMemberKeys().equals(Sets.newHashSet("nbt_end"))) {
            return ValueTypeNbt.ValueNbt.of(EndTag.INSTANCE);
        }

        // In all other cases, assume we have a compound tag
        CompoundTag tag = new CompoundTag();
        for (String memberKey : value.getMemberKeys()) {
            IValue subValue = ValueTranslators.REGISTRY.translateFromGraal(context, value.getMember(memberKey), exceptionFactory, valueDeseralizationContext);
            tag.put(memberKey, ValueTranslators.REGISTRY.translateToNbt(context, subValue, exceptionFactory));
        }
        return ValueTypeNbt.ValueNbt.of(tag);
    }

    @Override
    public Tag translateToNbt(Context context, ValueTypeNbt.ValueNbt value, IEvaluationExceptionFactory exceptionFactory) throws EvaluationException {
        return value.getRawValue().orElseThrow();
    }
}
