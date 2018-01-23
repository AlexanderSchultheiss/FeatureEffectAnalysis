package net.ssehub.kernel_haven.fe_analysis.config_relevancy;

import net.ssehub.kernel_haven.util.io.TableElement;
import net.ssehub.kernel_haven.util.io.TableRow;
import net.ssehub.kernel_haven.util.logic.Formula;

/**
 * Data type / result of the {@link ConfigRelevancyChecker}. Contains:
 * <ul>
 *     <li>The name of the checked variable</li>
 *     <li>The Integer value of a past configuration (may be <tt>null</tt>)</li>
 *     <li>The (evaluated) feature effect constraint</li>
 *     <li>A description/rating of the situation</li>
 * </ul>
 * @author Adam
 *
 */
@TableRow
public class VariableRelevance {

    /**
     * Description/rating of a past configuration of a single variable.
     * @author El-Sharkawy
     *
     */
    public static enum Relevance {
        
        SET_AND_RELEVANT("Configured and relevant"),
        SET_AND_IRRELEVANT("Configured, but not relevant"),
        NOT_SET_AND_RELEVANT("Not Configured, but relevant"),
        NOT_SET_AND_IRRELEVANT("Not Configured, not relevant"),
        UNKOWN("Unkown (couldn't solve feature effect)");
        
        private String name;
        
        /**
         * Enum constructor to specify the description.
         * @param name The description.
         */
        private Relevance(String name) {
            this.name = name;
        }
        
        @Override
        public String toString() {
            return name;
        }
        
    }
    
    private String variable;
    
    private Relevance relevance;
    
    private Formula featureEffect;
    
    private Integer value;
    
    /**
     * Sole constructor.
     * @param variable The name of the variable
     * @param relevance The description of the situation.
     * @param featureEffect The (evaluated) feature effect constraint.
     * @param value The historical configuration of the variable.
     */
    public VariableRelevance(String variable, Relevance relevance, Formula featureEffect, Integer value) {
        this.variable = variable;
        this.relevance = relevance;
        this.featureEffect = featureEffect;
        this.value = value;
    }

    /**
     * Returns the name of the variable.
     * @return The name of the variable.
     */
    @TableElement(index = 0, name = "Variable")
    public String getVariable() {
        return variable;
    }
    
    /**
     * Returns the historical value of the variable.
     * @return The historical value of the variable.
     */
    @TableElement(index = 1, name = "Value")
    public Integer getValue() {
        return value;
    }
    
    /**
     * Returns the (evaluated) feature effect constraint of the variable.
     * @return The h(evaluated) feature effect constraint of the variable.
     */
    @TableElement(index = 2, name = "Feature Effect")
    public Formula getFeatureEffect() {
        return featureEffect;
    }
    
    /**
     * Returns the description of the situation.
     * @return The description of the situation.
     */
    @TableElement(index = 3, name = "Relevance")
    public Relevance getRelevance() {
        return relevance;
    }
    
}
