package com.tuplejump.stargate.util;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import org.cassandraunit.dataset.CQLDataSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

/**
 * User: satya
 * <p/>
 * Dataloader implementation to use with CQLUnitD
 */
public class CQLDataLoaderD {

    private static final Logger log = LoggerFactory.getLogger(CQLDataLoaderD.class);
    public static final String DEFAULT_KEYSPACE_NAME = "cassandraunitkeyspace";

    protected Map<String, Integer> hostsAndPorts;
    protected Session session;


    CQLDataLoaderD(Map<String, Integer> hostsAndPorts) {
        this.hostsAndPorts = hostsAndPorts;
    }

    public Session createSession() {
        Cluster.Builder builder = new Cluster.Builder();
        Set<Map.Entry<String, Integer>> entries = hostsAndPorts.entrySet();
        for (Map.Entry<String, Integer> entry : entries) {
            builder.addContactPoints(entry.getKey()).withPort(entry.getValue());
        }
        Cluster cluster = builder.build();
        session = cluster.connect();
        return session;
    }

    public void load(CQLDataSet dataSet) {
        initKeyspaceContext(session, dataSet);

        log.debug("loading data");
        for (String query : dataSet.getCQLStatements()) {
            log.debug("executing : " + query);
            session.execute(query);
        }

        if (dataSet.getKeyspaceName() != null) {
            String useQuery = "use " + dataSet.getKeyspaceName();
            session.execute(useQuery);
        }
    }

    protected void initKeyspaceContext(Session session, CQLDataSet dataSet) {
        String keyspaceName = DEFAULT_KEYSPACE_NAME;
        if (dataSet.getKeyspaceName() != null) {
            keyspaceName = dataSet.getKeyspaceName();
        }

        log.debug("initKeyspaceContext : " +
                "keyspaceDeletion=" + dataSet.isKeyspaceDeletion() +
                "keyspaceCreation=" + dataSet.isKeyspaceCreation() +
                ";keyspaceName=" + keyspaceName);

        if (dataSet.isKeyspaceDeletion()) {
            String selectQuery = "SELECT keyspace_name FROM system.schema_keyspaces where keyspace_name='" + keyspaceName + "'";
            ResultSet keyspaceQueryResult = session.execute(selectQuery);
            if (keyspaceQueryResult.iterator().hasNext()) {
                String dropQuery = "DROP KEYSPACE " + keyspaceName;
                log.debug("executing : " + dropQuery);
                session.execute(dropQuery);
            }
        }

        if (dataSet.isKeyspaceCreation()) {
            String createQuery = "CREATE KEYSPACE " + keyspaceName + " WITH replication={'class' : 'SimpleStrategy', 'replication_factor':1}";
            log.debug("executing : " + createQuery);
            session.execute(createQuery);
        }

        String useQuery = "USE " + keyspaceName;
        log.debug("executing : " + useQuery);
        session.execute(useQuery);
    }

}
