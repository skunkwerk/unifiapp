package io.realm;


import com.unifiapp.model.*;
import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.internal.ColumnType;
import io.realm.internal.ImplicitTransaction;
import io.realm.internal.LinkView;
import io.realm.internal.Row;
import io.realm.internal.Table;
import java.util.*;

public class ConnectivityRealmProxy extends Connectivity {

    @Override
    public boolean isData_connection_active() {
        realm.checkIfValid();
        return (boolean) row.getBoolean(Realm.columnIndices.get("Connectivity").get("data_connection_active"));
    }

    @Override
    public void setData_connection_active(boolean value) {
        realm.checkIfValid();
        row.setBoolean(Realm.columnIndices.get("Connectivity").get("data_connection_active"), (boolean) value);
    }

    @Override
    public boolean isWifi_on() {
        realm.checkIfValid();
        return (boolean) row.getBoolean(Realm.columnIndices.get("Connectivity").get("wifi_on"));
    }

    @Override
    public void setWifi_on(boolean value) {
        realm.checkIfValid();
        row.setBoolean(Realm.columnIndices.get("Connectivity").get("wifi_on"), (boolean) value);
    }

    @Override
    public String getConnection_type() {
        realm.checkIfValid();
        return (java.lang.String) row.getString(Realm.columnIndices.get("Connectivity").get("connection_type"));
    }

    @Override
    public void setConnection_type(String value) {
        realm.checkIfValid();
        row.setString(Realm.columnIndices.get("Connectivity").get("connection_type"), (String) value);
    }

    @Override
    public java.util.Date getDatetime() {
        realm.checkIfValid();
        return (java.util.Date) row.getDate(Realm.columnIndices.get("Connectivity").get("datetime"));
    }

    @Override
    public void setDatetime(java.util.Date value) {
        realm.checkIfValid();
        row.setDate(Realm.columnIndices.get("Connectivity").get("datetime"), (Date) value);
    }

    public static Table initTable(ImplicitTransaction transaction) {
        if(!transaction.hasTable("class_Connectivity")) {
            Table table = transaction.getTable("class_Connectivity");
            table.addColumn(ColumnType.BOOLEAN, "data_connection_active");
            table.addColumn(ColumnType.BOOLEAN, "wifi_on");
            table.addColumn(ColumnType.STRING, "connection_type");
            table.addColumn(ColumnType.DATE, "datetime");
            return table;
        }
        return transaction.getTable("class_Connectivity");
    }

    public static void validateTable(ImplicitTransaction transaction) {
        if(transaction.hasTable("class_Connectivity")) {
            Table table = transaction.getTable("class_Connectivity");
            if(table.getColumnCount() != 4) {
                throw new IllegalStateException("Column count does not match");
            }
            Map<String, ColumnType> columnTypes = new HashMap<String, ColumnType>();
            for(long i = 0; i < 4; i++) {
                columnTypes.put(table.getColumnName(i), table.getColumnType(i));
            }
            if (!columnTypes.containsKey("data_connection_active")) {
                throw new IllegalStateException("Missing column 'data_connection_active'");
            }
            if (columnTypes.get("data_connection_active") != ColumnType.BOOLEAN) {
                throw new IllegalStateException("Invalid type 'boolean' for column 'data_connection_active'");
            }
            if (!columnTypes.containsKey("wifi_on")) {
                throw new IllegalStateException("Missing column 'wifi_on'");
            }
            if (columnTypes.get("wifi_on") != ColumnType.BOOLEAN) {
                throw new IllegalStateException("Invalid type 'boolean' for column 'wifi_on'");
            }
            if (!columnTypes.containsKey("connection_type")) {
                throw new IllegalStateException("Missing column 'connection_type'");
            }
            if (columnTypes.get("connection_type") != ColumnType.STRING) {
                throw new IllegalStateException("Invalid type 'String' for column 'connection_type'");
            }
            if (!columnTypes.containsKey("datetime")) {
                throw new IllegalStateException("Missing column 'datetime'");
            }
            if (columnTypes.get("datetime") != ColumnType.DATE) {
                throw new IllegalStateException("Invalid type 'Date' for column 'datetime'");
            }
        }
    }

    public static List<String> getFieldNames() {
        return Arrays.asList("data_connection_active", "wifi_on", "connection_type", "datetime");
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder("Connectivity = [");
        stringBuilder.append("{data_connection_active:");
        stringBuilder.append(isData_connection_active());
        stringBuilder.append("}");
        stringBuilder.append(",");
        stringBuilder.append("{wifi_on:");
        stringBuilder.append(isWifi_on());
        stringBuilder.append("}");
        stringBuilder.append(",");
        stringBuilder.append("{connection_type:");
        stringBuilder.append(getConnection_type());
        stringBuilder.append("}");
        stringBuilder.append(",");
        stringBuilder.append("{datetime:");
        stringBuilder.append(getDatetime());
        stringBuilder.append("}");
        stringBuilder.append("]");
        return stringBuilder.toString();
    }

    @Override
    public int hashCode() {
        String realmName = realm.getPath();
        String tableName = row.getTable().getName();
        long rowIndex = row.getIndex();

        int result = 17;
        result = 31 * result + ((realmName != null) ? realmName.hashCode() : 0);
        result = 31 * result + ((tableName != null) ? tableName.hashCode() : 0);
        result = 31 * result + (int) (rowIndex ^ (rowIndex >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConnectivityRealmProxy aConnectivity = (ConnectivityRealmProxy)o;

        String path = realm.getPath();
        String otherPath = aConnectivity.realm.getPath();
        if (path != null ? !path.equals(otherPath) : otherPath != null) return false;;

        String tableName = row.getTable().getName();
        String otherTableName = aConnectivity.row.getTable().getName();
        if (tableName != null ? !tableName.equals(otherTableName) : otherTableName != null) return false;

        return true;
    }

}
