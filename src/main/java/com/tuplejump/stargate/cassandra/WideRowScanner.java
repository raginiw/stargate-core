package com.tuplejump.stargate.cassandra;

import com.tuplejump.stargate.Fields;
import com.tuplejump.stargate.RowIndex;
import org.apache.cassandra.config.ColumnDefinition;
import org.apache.cassandra.db.*;
import org.apache.cassandra.db.filter.ColumnSlice;
import org.apache.cassandra.db.filter.ExtendedFilter;
import org.apache.cassandra.db.filter.IDiskAtomFilter;
import org.apache.cassandra.db.filter.SliceQueryFilter;
import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.db.marshal.CompositeType;
import org.apache.cassandra.db.marshal.UTF8Type;
import org.apache.cassandra.utils.ByteBufferUtil;
import org.apache.cassandra.utils.Pair;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TopDocs;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * User: satya
 * Iterator to read rows from wide tables using the lucene search results.
 */
public class WideRowScanner extends RowScanner {

    public WideRowScanner(SearchSupport searchSupport, ColumnFamilyStore table, IndexSearcher searcher, ExtendedFilter filter, TopDocs topDocs, boolean needsFiltering) throws Exception {
        super(searchSupport, table, searcher, filter, topDocs, needsFiltering);
    }

    @Override
    protected void addMetaColumn(Column firstColumn, String colName, Float score, ColumnFamily cleanColumnFamily) {
        CompositeType baseComparator = (CompositeType) table.getComparator();
        ByteBuffer[] components = baseComparator.split(firstColumn.name());
        int prefixSize = baseComparator.types.size() - (table.metadata.getCfDef().hasCollections ? 2 : 1);
        CompositeType.Builder builder = baseComparator.builder();
        for (int i = 0; i < prefixSize; i++)
            builder.add(components[i]);
        builder.add(UTF8Type.instance.decompose(colName));
        ByteBuffer finalColumnName = builder.build();
        Column scoreColumn = new Column(finalColumnName, UTF8Type.instance.decompose("{\"score\":" + score.toString() + "}"));
        cleanColumnFamily.addColumn(scoreColumn);
    }


    protected Pair<DecoratedKey, IDiskAtomFilter> getFilterAndKey(ByteBuffer primaryKey, SliceQueryFilter sliceQueryFilter) {
        ByteBuffer[] components = getCompositePKComponents(table, primaryKey);
        ByteBuffer rowKey = getRowKeyFromPKComponents(components);
        DecoratedKey dk = table.partitioner.decorateKey(rowKey);
        final CompositeType baseComparator = (CompositeType) table.getComparator();
        int prefixSize = baseComparator.types.size() - (table.metadata.getCfDef().hasCollections ? 2 : 1);

        CompositeType.Builder builder = baseComparator.builder();

        for (int i = 0; i < prefixSize; i++)
            builder.add(components[i + 1]);

        ByteBuffer start = builder.build();
        if (!sliceQueryFilter.maySelectPrefix(table.getComparator(), start)) return null;

        ArrayList<ColumnSlice> allSlices = new ArrayList<>();
        ColumnSlice dataSlice = new ColumnSlice(start, builder.buildAsEndOfRange());
        if (table.metadata.hasStaticColumns()) {
            ColumnSlice staticSlice = new ColumnSlice(ByteBufferUtil.EMPTY_BYTE_BUFFER, table.metadata.getStaticColumnNameBuilder().buildAsEndOfRange());
            allSlices.add(staticSlice);
        }
        allSlices.add(dataSlice);
        ColumnSlice[] slices = new ColumnSlice[allSlices.size()];
        allSlices.toArray(slices);
        IDiskAtomFilter dataFilter = new SliceQueryFilter(slices, false, Integer.MAX_VALUE, table.metadata.clusteringKeyColumns().size());
        return Pair.create(dk, dataFilter);
    }

    public static ByteBuffer[] getCompositePKComponents(ColumnFamilyStore baseCfs, ByteBuffer pk) {
        CompositeType baseComparator = (CompositeType) baseCfs.getComparator();
        return baseComparator.split(pk);
    }

    public static ByteBuffer getRowKeyFromPKComponents(ByteBuffer[] pkComponents) {
        return pkComponents[0];
    }


}
