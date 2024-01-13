package org.cyclops.integratedscripting.evaluate.translation.translator;

import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.cyclops.integrateddynamics.api.evaluate.EvaluationException;
import org.cyclops.integrateddynamics.api.evaluate.operator.IOperator;
import org.cyclops.integrateddynamics.api.evaluate.operator.IOperatorSerializer;
import org.cyclops.integrateddynamics.api.evaluate.variable.IValueType;
import org.cyclops.integrateddynamics.api.evaluate.variable.IVariable;
import org.cyclops.integrateddynamics.core.evaluate.operator.OperatorBase;
import org.cyclops.integrateddynamics.core.evaluate.operator.Operators;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypeOperator;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypes;
import org.cyclops.integratedscripting.Reference;
import org.cyclops.integratedscripting.api.evaluate.translation.IEvaluationExceptionFactory;
import org.cyclops.integratedscripting.api.evaluate.translation.IValueTranslator;
import org.cyclops.integratedscripting.evaluate.translation.ValueTranslators;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;

/**
 * @author rubensworks
 */
public class ValueTranslatorOperator implements IValueTranslator<ValueTypeOperator.ValueOperator> {

    @Override
    public IValueType<?> getValueType() {
        return ValueTypes.OPERATOR;
    }

    @Override
    public boolean canHandleGraalValue(Value value) {
        return value.canExecute();
    }

    @Override
    public boolean canTranslateNbt() {
        return false;
    }

    @Override
    public Value translateToGraal(Context context, ValueTypeOperator.ValueOperator value, IEvaluationExceptionFactory exceptionFactory) throws EvaluationException {
        return context.asValue(new OperatorProxyExecutable(context, value, exceptionFactory));
    }

    @Override
    public ValueTypeOperator.ValueOperator translateFromGraal(Context context, Value value, IEvaluationExceptionFactory exceptionFactory) throws EvaluationException {
        // Unwrap the value if it was translated in the opposite direction before.
        if (value.isProxyObject()) {
            try {
                OperatorProxyExecutable cast = value.asProxyObject();
                return cast.getValue();
            } catch (ClassCastException classCastException) {
                // Fallback to case below
            }
        }

        // Determine input args of the function
        int argCount = value.getMember("length").asInt();
        IValueType[] inputValueTypes = new IValueType[argCount];
        for (int i = 0; i < argCount; i++) {
            inputValueTypes[i] = ValueTypes.CATEGORY_ANY;
        }

        // Create custom operator
        return ValueTypeOperator.ValueOperator.of(new GraalOperator(inputValueTypes, args -> {
            IVariable[] variables = args.getVariables();
            Value[] values = new Value[variables.length];
            context.resetLimits();
            for (int i = 0; i < variables.length; i++) {
                try {
                    values[i] = ValueTranslators.REGISTRY.translateToGraal(context, variables[i].getValue(), exceptionFactory);
                } catch (PolyglotException e) {
                    throw exceptionFactory.createError(e.getMessage());
                }
            }
            Value returnValue;
            try {
                returnValue = value.execute((Object[]) values);
            } catch (PolyglotException e) {
                throw exceptionFactory.createError(e.getMessage());
            }
            return ValueTranslators.REGISTRY.translateFromGraal(context, returnValue, exceptionFactory);
        }));
    }

    @Override
    public Tag translateToNbt(Context context, ValueTypeOperator.ValueOperator value, IEvaluationExceptionFactory exceptionFactory) throws EvaluationException {
        throw new UnsupportedOperationException("translateToNbt is not supported");
    }

    public static class GraalOperator extends OperatorBase {
        protected GraalOperator(IValueType[] inputTypes, IFunction function) {
            super("graal", "graal", "graal", null, false, inputTypes, ValueTypes.CATEGORY_ANY, function, null);
        }

        @Override
        protected String getUnlocalizedType() {
            return "integratedscript";
        }

        @Override
        protected String getModId() {
            return Reference.MOD_ID;
        }

        public static class Serializer implements IOperatorSerializer<GraalOperator> {

            @Override
            public boolean canHandle(IOperator operator) {
                return operator instanceof GraalOperator;
            }

            @Override
            public ResourceLocation getUniqueName() {
                return new ResourceLocation(Reference.MOD_ID, "graal");
            }

            @Override
            public Tag serialize(GraalOperator operator) {
                ListTag list = new ListTag();
                for (int i = 0; i < operator.getInputTypes().length; i++) {
                    list.add(StringTag.valueOf(operator.getInputTypes()[i].getUniqueName().toString()));
                }
                return list;
            }

            @Override
            public GraalOperator deserialize(Tag value) throws EvaluationException {
                ListTag listTag = (ListTag) value;
                IValueType[] inputTypes = new IValueType[listTag.size()];
                for (int i = 0; i < listTag.size(); i++) {
                    inputTypes[i] = ValueTypes.REGISTRY.getValueType(new ResourceLocation(listTag.getString(i)));
                }
                return new GraalOperator(inputTypes, (variables) -> {
                    throw new EvaluationException(Component.translatable("operator.integratedscripting.error.no_graal_serialization"));
                });
            }
        }
    }

    static {
        Operators.REGISTRY.registerSerializer(new GraalOperator.Serializer());
    }

}
