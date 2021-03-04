/*
 * Copyright 2021 University of Hildesheim, Software Systems Engineering
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
package net.ssehub.kernel_haven.fe_analysis.pcs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import net.ssehub.kernel_haven.fe_analysis.pcs.CodeBlockAnalysis.CodeBlock;
import net.ssehub.kernel_haven.util.null_checks.NonNull;

/**
 * 
 * Stores computes {@link CodeBlock} results.
 * @author El-Sharkawy
 */
class CodeBlockStore {

    private Map<String, List<@NonNull CodeBlock>> map = new HashMap<>();
    
    /**
     * Adds a new {@link CodeBlock} to this store.
     * @param block The newly computed {@link CodeBlock} to store.
     */
    public void add(@NonNull CodeBlock block) {
        List<@NonNull CodeBlock> blocksOfFile = map.get(block.getPath());
        if (null == blocksOfFile) {
            blocksOfFile = new ArrayList<>();
            map.put(block.getPath(), blocksOfFile);
        }
        blocksOfFile.add(block);
    }
    
    /**
     * Returns a sorted list of all stored {@link CodeBlock}s.
     * @return A sorted list of all stored {@link CodeBlock}s.
     */
    public Stream<@NonNull CodeBlock> getOrderedStream() {
        List<@NonNull CodeBlock> orderedList = new ArrayList<>();
        map.keySet().stream()
            .sorted()
            .map(p -> map.get(p))
            .forEach(orderedList::addAll);
        return orderedList.stream();
    }
}
