/*
 * Copyright 2017-2019 University of Hildesheim, Software Systems Engineering
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.ssehub.kernel_haven.fe_analysis;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.ssehub.kernel_haven.util.null_checks.NonNull;

/**
 * String operations, e.g., regular expressions, used in this package.
 * @author El-Sharkawy
 *
 */
public class StringUtils {
    
    private static final String OPERATOR_REGEX = "(=|!=|<|<=|>|>=)";
    
    /**
     * Avoids instantiation.
     */
    private StringUtils() {}
    
    /**
     * Gets the last (starting index) of an operator, which may be used inside variable names.
     * @param name The variable name (or a formula) to test.
     * @return The index of the last operator, or -1 if the name won't have any operators.
     */
    public static int getLastOperatorIndex(@NonNull String name) {
        Matcher matcher = Pattern.compile(OPERATOR_REGEX).matcher(name);
        int result;
        if (matcher.find()) {
            String identified = matcher.group(matcher.groupCount());
            result = matcher.end() - identified.length();
        } else {
            result = -1;
        }
        
        return result;
    }

}
