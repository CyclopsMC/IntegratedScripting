package org.cyclops.integratedscripting.evaluate.translation;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import org.cyclops.integrateddynamics.Reference;
import org.cyclops.integrateddynamics.api.evaluate.EvaluationException;
import org.cyclops.integrateddynamics.api.evaluate.variable.IValue;
import org.cyclops.integrateddynamics.api.evaluate.variable.IValueType;
import org.cyclops.integrateddynamics.api.evaluate.variable.ValueDeseralizationContext;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueBase;
import org.cyclops.integrateddynamics.core.logicprogrammer.ValueTypeLPElementBase;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Dummy value type
 * @author rubensworks
 */
public class DummyValueType implements IValueType<DummyValueType.DummyValue> {

    public static final DummyValueType TYPE = new DummyValueType();

    @Override
    public boolean isCategory() {
        return false;
    }

    @Override
    public boolean isObject() {
        return false;
    }

    @Override
    public DummyValue getDefault() {
        return null;
    }

    @Override
    public String getTypeName() {
        return "boolean";
    }

    @Override
    public ResourceLocation getUniqueName() {
        return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "dummy");
    }

    @Override
    public String getTranslationKey() {
        return "boolean";
    }

    @Override
    public void loadTooltip(List<Component> lines, boolean appendOptionalInfo, DummyValue value) {

    }

    @Override
    public MutableComponent toCompactString(DummyValue value) {
        return Component.literal("dummy");
    }

    @Override
    public int getDisplayColor() {
        return 0;
    }

    @Override
    public ChatFormatting getDisplayColorFormat() {
        return ChatFormatting.WHITE;
    }

    @Override
    public boolean correspondsTo(IValueType<?> valueType) {
        return false;
    }

    @Override
    public Tag serialize(ValueDeseralizationContext valueDeseralizationContext, DummyValue value) {
        return null;
    }

    @Nullable
    @Override
    public Component canDeserialize(ValueDeseralizationContext valueDeseralizationContext, Tag value) {
        return null;
    }

    @Override
    public DummyValue deserialize(ValueDeseralizationContext valueDeseralizationContext, Tag value) {
        return null;
    }

    @Override
    public DummyValue materialize(DummyValue value) {
        return value;
    }

    @Override
    public String toString(DummyValue value) {
        return "";
    }

    @Override
    public DummyValue parseString(String value) throws EvaluationException {
        return DummyValue.of();
    }

    @Override
    public ValueTypeLPElementBase createLogicProgrammerElement() {
        return null;
    }

    @Override
    public DummyValue cast(IValue value) throws EvaluationException {
        return (DummyValue) value;
    }

    public static class DummyValue extends ValueBase {

        private DummyValue() {
            super(TYPE);
        }

        public static DummyValue of() {
            return new DummyValue();
        }

    }

}
