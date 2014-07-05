/*
 * Copyright 2014, Stratio.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tuplejump.stargate.lucene.query;

import com.tuplejump.stargate.lucene.Options;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.Iterator;
import java.util.List;

/**
 * A sorting of fields for a search.
 */
public class Sorting implements Iterable<SortingField> {

    /**
     * How to sort each field
     */
    private final List<SortingField> sortingFields;

    @JsonCreator
    public Sorting(@JsonProperty("fields") List<SortingField> sortingFields) {
        this.sortingFields = sortingFields;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<SortingField> iterator() {
        return sortingFields.iterator();
    }

    /**
     * Returns the {@link SortingField}s to be used.
     *
     * @return
     */
    public List<SortingField> getSortingFields() {
        return sortingFields;
    }

    /**
     * Returns the Lucene's {@link Sort} representing this {@link Sorting}.
     *
     * @param schema
     * @return the Lucene's {@link Sort} representing this {@link Sorting}.
     */
    public Sort sort(Options schema) {
        SortField[] sortFields = new SortField[sortingFields.size()];
        for (int i = 0; i < sortingFields.size(); i++) {
            sortFields[i] = sortingFields.get(i).sortField(schema);
        }
        return new Sort(sortFields);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Sorting [sortingFields=");
        builder.append(sortingFields);
        builder.append("]");
        return builder.toString();
    }

}