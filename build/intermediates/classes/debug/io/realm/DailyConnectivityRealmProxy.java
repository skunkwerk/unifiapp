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

public class DailyConnectivityRealmProxy extends DailyConnectivity {

    @Override
    public float getData_connection_active_average() {
        realm.checkIfValid();
        return (float) row.getFloat(Realm.columnIndices.get("DailyConnectivity").get("data_connection_active_average"));
    }

    @Override
    public void setData_connection_active_average(float value) {
        realm.checkIfValid();
        row.setFloat(Realm.columnIndices.get("DailyConnectivity").get("data_connection_active_average"), (float) value);
    }

    @Override
    public float getWifi_on_average() {
        realm.checkIfValid();
        return (float) row.getFloat(Realm.columnIndices.get("DailyConnectivity").get("wifi_on_average"));
    }

    @Override
    public void setWifi_on_average(float value) {
        realm.checkIfValid();
        row.setFloat(Realm.columnIndices.get("DailyConnectivity").get("wifi_on_average"), (float) value);
    }

    @Override
    public float getWifi_connected_average() {
        realm.checkIfValid();
        return (float) row.getFloat(Realm.columnIndices.get("DailyConnectivity").get("wifi_connected_average"));
    }

    @Override
    public void setWifi_connected_average(float value) {
        realm.checkIfValid();
        row.setFloat(Realm.columnIndices.get("DailyConnectivity").get("wifi_connected_average"), (float) value);
    }

    @Override
    public float getMobile_data_connected_average() {
        realm.checkIfValid();
        return (float) row.getFloat(Realm.columnIndices.get("DailyConnectivity").get("mobile_data_connected_average"));
    }

    @Override
    public void setMobile_data_connected_average(float value) {
        realm.checkIfValid();
        row.setFloat(Realm.columnIndices.get("DailyConnectivity").get("mobile_data_connected_average"), (float) value);
    }

    @Override
    public java.util.Date getDate() {
        realm.checkIfValid();
        return (java.util.Date) row.getDate(Realm.columnIndices.get("DailyConnectivity").get("date"));
    }

    @Override
    public void setDate(java.util.Date value) {
        realm.checkIfValid();
        row.setDate(Realm.columnIndices.get("DailyConnectivity").get("date"), (Date) value);
    }

    @Override
    public String getAnonymous_customer_id_hash() {
        realm.checkIfValid();
        return (java.lang.String) row.getString(Realm.columnIndices.get("DailyConnectivity").get("anonymous_customer_id_hash"));
    }

    @Override
    public void setAnonymous_customer_id_hash(String value) {
        realm.checkIfValid();
        row.setString(Realm.columnIndices.get("DailyConnectivity").get("anonymous_customer_id_hash"), (String) value);
    }

    public static Table initTable(ImplicitTransaction transaction) {
        if(!transaction.hasTable("class_DailyConnectivity")) {
            Table table = transaction.getTable("class_DailyConnectivity");
            table.addColumn(ColumnType.FLOAT, "data_connection_active_average");
            table.addColumn(ColumnType.FLOAT, "wifi_on_average");
            table.addColumn(ColumnType.FLOAT, "wifi_connected_average");
            table.addColumn(ColumnType.FLOAT, "mobile_data_connected_average");
            table.addColumn(ColumnType.DATE, "date");
            table.addColumn(ColumnType.STRING, "anonymous_customer_id_hash");
            return table;
        }
        return transaction.getTable("class_DailyConnectivity");
    }

    public static void validateTable(ImplicitTransaction transaction) {
        if(transaction.hasTable("class_DailyConnectivity")) {
            Table table = transaction.getTable("class_DailyConnectivity");
            if(table.getColumnCount() != 6) {
                throw new IllegalStateException("Column count does not match");
            }
            Map<String, ColumnType> columnTypes = new HashMap<String, ColumnType>();
            for(long i = 0; i < 6; i++) {
                columnTypes.put(table.getColumnName(i), table.getColumnType(i));
            }
            if (!columnTypes.containsKey("data_connection_active_average")) {
                throw new IllegalStateException("Missing column 'data_connection_active_average'");
            }
            if (columnTypes.get("data_connection_active_average") != ColumnType.FLOAT) {
                throw new IllegalStateException("Invalid type 'float' for column 'data_connection_active_average'");
            }
            if (!columnTypes.containsKey("wifi_on_average")) {
                throw new IllegalStateException("Missing column 'wifi_on_average'");
            }
            if (columnTypes.get("wifi_on_average") != ColumnType.FLOAT) {
                throw new IllegalStateException("Invalid type 'float' for column 'wifi_on_average'");
            }
            if (!columnTypes.containsKey("wifi_connected_average")) {
                throw new IllegalStateException("Missing column 'wifi_connected_average'");
            }
            if (columnTypes.get("wifi_connected_average") != ColumnType.FLOAT) {
                throw new IllegalStateException("Invalid type 'float' for column 'wifi_connected_average'");
            }
            if (!columnTypes.containsKey("mobile_data_connected_average")) {
                throw new IllegalStateException("Missing column 'mobile_data_connected_average'");
            }
            if (columnTypes.get("mobile_data_connected_average") != ColumnType.FLOAT) {
                throw new IllegalStateException("Invalid type 'float' for column 'mobile_data_connected_average'");
            }
            if (!columnTypes.containsKey("date")) {
                throw new IllegalStateException("Missing column 'date'");
            }
            if (columnTypes.get("date") != ColumnType.DATE) {
                throw new IllegalStateException("Invalid type 'Date' for column 'date'");
            }
            if (!columnTypes.containsKey("anonymous_customer_id_hash")) {
                throw new IllegalStateException("Missing column 'anonymous_customer_id_hash'");
            }
            if (columnTypes.get("anonymous_customer_id_hash") != ColumnType.STRING) {
                throw new IllegalStateException("Invalid type 'String' for column 'anonymous_customer_id_hash'");
            }
        }
    }

    public static List<String> getFieldNames() {
        return Arrays.asList("data_connection_active_average", "wifi_on_average", "wifi_connected_average", "mobile_data_connected_average", "date", "anonymous_customer_id_hash");
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder("DailyConnectivity = [");
        stringBuilder.append("{data_connection_active_average:");
        stringBuilder.append(getData_connection_active_average());
        stringBuilder.append("}");
        stringBuilder.append(",");
        stringBuilder.append("{wifi_on_average:");
        stringBuilder.append(getWifi_on_average());
        stringBuilder.append("}");
        stringBuilder.append(",");
        stringBuilder.append("{wifi_connected_average:");
        stringBuilder.append(getWifi_connected_average());
        stringBuilder.append("}");
        stringBuilder.append(",");
        stringBuilder.append("{mobile_data_connected_average:");
        stringBuilder.append(getMobile_data_connected_average());
        stringBuilder.append("}");
        stringBuilder.append(",");
        stringBuilder.append("{date:");
        stringBuilder.append(getDate());
        stringBuilder.append("}");
        stringBuilder.append(",");
        stringBuilder.append("{anonymous_customer_id_hash:");
        stringBuilder.append(getAnonymous_customer_id_hash());
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
        DailyConnectivityRealmProxy aDailyConnectivity = (DailyConnectivityRealmProxy)o;

        String path = realm.getPath();
        String otherPath = aDailyConnectivity.realm.getPath();
        if (path != null ? !path.equals(otherPath) : otherPath != null) return false;;

        String tableName = row.getTable().getName();
        String otherTableName = aDailyConnectivity.row.getTable().getName();
        if (tableName != null ? !tableName.equals(otherTableName) : otherTableName != null) return false;

        return true;
    }

}
