package com.tuplejump.stargate.cassandra;

import com.tuplejump.stargate.Fields;
import com.tuplejump.stargate.RowIndex;
import org.apache.cassandra.config.ColumnDefinition;
import org.apache.cassandra.db.*;
import org.apache.cassandra.db.filter.ExtendedFilter;
import org.apache.cassandra.db.filter.IDiskAtomFilter;
import org.apache.cassandra.db.filter.SliceQueryFilter;
import org.apache.cassandra.db.marshal.UTF8Type;
import org.apache.cassandra.utils.ByteBufferUtil;
import org.apache.cassandra.utils.Pair;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TopDocs;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * User: satya
 */
public class SimpleRowScanner extends RowScanner {

    public SimpleRowScanner(SearchSupport searchSupport, ColumnFamilyStore table, IndexSearcher searcher, ExtendedFilter filter, TopDocs topDocs, boolean needsFiltering) throws Exception {
        super(searchSupport, table, searcher, filter, topDocs, needsFiltering);
    }

    @Override
    protected void addMetaColumn(Column firstColumn, String colName, Float score, ColumnFamily cleanColumnFamily) {
        Column scoreColumn = new Column(UTF8Type.instance.decompose(colName), UTF8Type.instance.decompose("{\"score\":" + score.toString() + "}"));
        cleanColumnFamily.addColumn(scoreColumn);
    }

    protected Pair<DecoratedKey, IDiskAtomFilter> getFilterAndKey(ByteBuffer primaryKey, SliceQueryFilter sliceQueryFilter) {
        DecoratedKey dk = table.partitioner.decorateKey(primaryKey);
        IDiskAtomFilter dataFilter = filter.columnFilter(primaryKey);
        return Pair.create(dk, dataFilter);
    }

}
