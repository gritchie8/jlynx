package com.github.jlynx;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

class PK {

    private static Map<String, Set<String>> primaryKeys = new TreeMap<String, Set<String>>();

    static Set<String> getPK(Connection conn, String table) throws SQLException {

        String url = conn.getMetaData().getURL();
        String key = url + "|" + table;
        Set<String> pks = primaryKeys.get(key);

        if (pks == null) {

            ResultSet resultSet = conn.getMetaData().getPrimaryKeys(null, null, table);

            pks = new HashSet<String>();
            String pk = null;
            while (resultSet.next()) {
                pk = resultSet.getString(4);
                pks.add(pk);
            }

            if (pk == null)
                throw new SQLException("Primary key expected for table: " + table);

            pks.remove("");
            pks.remove(null);
            if (!pks.isEmpty())
                primaryKeys.put(key, pks);

        }

        return primaryKeys.get(key);
    }

}
