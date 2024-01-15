package org.cyclops.integratedscripting.core.language;

import net.minecraft.client.resources.language.I18n;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import org.cyclops.cyclopscore.helper.L10NHelpers;
import org.cyclops.integrateddynamics.api.evaluate.operator.IOperator;
import org.cyclops.integrateddynamics.api.evaluate.variable.IValueType;
import org.cyclops.integrateddynamics.core.evaluate.operator.Operators;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypes;
import org.cyclops.integratedscripting.api.evaluate.translation.IValueTranslator;
import org.cyclops.integratedscripting.evaluate.translation.ValueTranslators;
import org.cyclops.integratedscripting.evaluate.translation.translator.ValueTranslatorObjectAdapter;

import java.util.Arrays;
import java.util.Map;

/**
 * Generates TypeScript definitions for all available operators and value types.
 * @author rubensworks
 */
public class TypeScriptTypingsGenerator {

    public String generate() {
        StringBuilder fileBuilder = new StringBuilder();

        // Static header
        fileBuilder.append("""
                /**
                 * Typings for Integrated Scripting.
                 * This file is auto-generated on server start, so do not modify this file!
                 */

                declare global {
                  var idContext: Context;
                }

                export interface Context {
                  ops: Operations;
                }

                """);

        // Global operators
        fileBuilder.append("export interface Operations {\n");
        for (Map.Entry<String, IOperator> entry : Operators.REGISTRY.getGlobalInteractOperators().entrySet()) {
            handleOperator(fileBuilder, entry.getKey(), entry.getValue(), false);
        }
        fileBuilder.append("}\n\n");

        // Value object types
        for (IValueType valueType : ValueTypes.REGISTRY.getValueTypes()) {
            IValueTranslator translator = ValueTranslators.REGISTRY.getValueTypeTranslator(valueType);
            if (valueType.isObject() && translator instanceof ValueTranslatorObjectAdapter translatorObjectAdapter) {
                // Value type description
                handleDescriptionComment(fileBuilder, valueType.getTranslationKey() + ".info");

                // Interface declaration
                fileBuilder.append("export interface ");
                fileBuilder.append(getObjectValueTypeInterfaceName(valueType));
                fileBuilder.append(" {\n");

                // Identifying member
                fileBuilder.append("  ");
                fileBuilder.append(translatorObjectAdapter.getKey());
                fileBuilder.append(": Record<string, any>;\n\n");

                // Scoped operators
                for (Map.Entry<String, IOperator> entry : Operators.REGISTRY.getScopedInteractOperators().get(valueType).entrySet()) {
                    handleOperator(fileBuilder, entry.getKey(), entry.getValue(), true);
                }

                fileBuilder.append("}\n\n");
            }
        }

        return fileBuilder.toString();
    }

    private void handleDescriptionComment(StringBuilder sb, String unlocalizedKey) {
        DistExecutor.callWhenOn(Dist.CLIENT, () -> () -> {
            if (I18n.exists(unlocalizedKey)) {
                sb.append("  /**\n");
                sb.append("   * ");
                sb.append(L10NHelpers.localize(unlocalizedKey));
                sb.append("\n");
                sb.append("   */\n");
            }
            return null;
        });
    }

    private void handleOperator(StringBuilder sb, String name, IOperator operator, boolean scoped) {
        // Operator description
        handleDescriptionComment(sb, operator.getTranslationKey() + ".info");

        // Operator signature
        sb.append(String.format("  %s: (%s) => %s;\n", name, this.generateArguments(operator, scoped), this.generateType(operator.getOutputType())));
    }

    private String generateArguments(IOperator operator, boolean scoped) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        IValueType[] inputTypes = operator.getInputTypes();
        if (scoped) {
            inputTypes = Arrays.copyOfRange(inputTypes, 1, inputTypes.length);
        }
        for (IValueType inputType : inputTypes) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(String.format("arg%s: %s", i++, this.generateType(inputType)));
        }
        return sb.toString();
    }

    private String generateType(IValueType<?> valueType) {
        if (ValueTypes.CATEGORY_NUMBER.correspondsTo(valueType)) {
            return "number";
        }
        if (valueType.isCategory()) {
            return "any";
        }
        if (ValueTypes.BOOLEAN == valueType) {
            return "boolean";
        }
        if (ValueTypes.STRING == valueType) {
            return "string";
        }
        if (ValueTypes.LIST == valueType) {
            return "any[]";
        }
        if (ValueTypes.NBT == valueType) {
            return "Record<string, any>";
        }
        if (ValueTypes.OPERATOR == valueType) {
            return "Function";
        }
        return getObjectValueTypeInterfaceName(valueType);
    }

    private String getObjectValueTypeInterfaceName(IValueType<?> valueType) {
        String name = valueType.getTypeName();
        return "Value" + Character.toTitleCase(name.charAt(0)) + name.substring(1);
    }

}
