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

public class DataUsageStateRealmProxy extends DataUsageState {

    @Override
    public float getLast_recorded_all_received_bytes_value() {
        realm.checkIfValid();
        return (float) row.getFloat(Realm.columnIndices.get("DataUsageState").get("last_recorded_all_received_bytes_value"));
    }

    @Override
    public void setLast_recorded_all_received_bytes_value(float value) {
        realm.checkIfValid();
        row.setFloat(Realm.columnIndices.get("DataUsageState").get("last_recorded_all_received_bytes_value"), (float) value);
    }

    @Override
    public float getLast_recorded_all_transmitted_bytes_value() {
        realm.checkIfValid();
        return (float) row.getFloat(Realm.columnIndices.get("DataUsageState").get("last_recorded_all_transmitted_bytes_value"));
    }

    @Override
    public void setLast_recorded_all_transmitted_bytes_value(float value) {
        realm.checkIfValid();
        row.setFloat(Realm.columnIndices.get("DataUsageState").get("last_recorded_all_transmitted_bytes_value"), (float) value);
    }

    @Override
    public float getLast_recorded_mobile_received_bytes_value() {
        realm.checkIfValid();
        return (float) row.getFloat(Realm.columnIndices.get("DataUsageState").get("last_recorded_mobile_received_bytes_value"));
    }

    @Override
    public void setLast_recorded_mobile_received_bytes_value(float value) {
        realm.checkIfValid();
        row.setFloat(Realm.columnIndices.get("DataUsageState").get("last_recorded_mobile_received_bytes_value"), (float) value);
    }

    @Override
    public float getLast_recorded_mobile_transmitted_bytes_value() {
        realm.checkIfValid();
        return (float) row.getFloat(Realm.columnIndices.get("DataUsageState").get("last_recorded_mobile_transmitted_bytes_value"));
    }

    @Override
    public void setLast_recorded_mobile_transmitted_bytes_value(float value) {
        realm.checkIfValid();
        row.setFloat(Realm.columnIndices.get("DataUsageState").get("last_recorded_mobile_transmitted_bytes_value"), (float) value);
    }

    @Override
    public java.util.Date getDatetime() {
        realm.checkIfValid();
        return (java.util.Date) row.getDate(Realm.columnIndices.get("DataUsageState").get("datetime"));
    }

    @Override
    public void setDatetime(java.util.Date value) {
        realm.checkIfValid();
        row.setDate(Realm.columnIndices.get("DataUsageState").get("datetime"), (Date) value);
    }

    public static Table initTable(ImplicitTransaction transaction) {
        if(!transaction.hasTable("class_DataUsageState")) {
            Table table = transaction.getTable("class_DataUsageState");
            table.addColumn(ColumnType.FLOAT, "last_recorded_all_received_bytes_value");
            table.addColumn(ColumnType.FLOAT, "last_recorded_all_transmitted_bytes_value");
            table.addColumn(ColumnType.FLOAT, "last_recorded_mobile_received_bytes_value");
            table.addColumn(ColumnType.FLOAT, "last_recorded_mobile_transmitted_bytes_value");
            table.addColumn(ColumnType.DATE, "datetime");
            return table;
        }
        return transaction.getTable("class_DataUsageState");
    }

    public static void validateTable(ImplicitTransaction transaction) {
        if(transaction.hasTable("class_DataUsageState")) {
            Table table = transaction.getTable("class_DataUsageState");
            if(table.getColumnCount() != 5) {
                throw new IllegalStateException("Column count does not match");
            }
            Map<String, ColumnType> columnTypes = new HashMap<String, ColumnType>();
            for(long i = 0; i < 5; i++) {
                columnTypes.put(table.getColumnName(i), table.getColumnType(i));
            }
            if (!columnTypes.containsKey("last_recorded_all_received_bytes_value")) {
                throw new IllegalStateException("Missing column 'last_recorded_all_received_bytes_value'");
            }
            if (columnTypes.get("last_recorded_all_received_bytes_value") != ColumnType.FLOAT) {
                throw new IllegalStateException("Invalid type 'float' for column 'last_recorded_all_received_bytes_value'");
            }
            if (!columnTypes.containsKey("last_recorded_all_transmitted_bytes_value")) {
                throw new IllegalStateException("Missing column 'last_recorded_all_transmitted_bytes_value'");
            }
            if (columnTypes.get("last_recorded_all_transmitted_bytes_value") != ColumnType.FLOAT) {
                throw new IllegalStateException("Invalid type 'float' for column 'last_recorded_all_transmitted_bytes_value'");
            }
            if (!columnTypes.containsKey("last_recorded_mobile_received_bytes_value")) {
                throw new IllegalStateException("Missing column 'last_recorded_mobile_received_bytes_value'");
            }
            if (columnTypes.get("last_recorded_mobile_received_bytes_value") != ColumnType.FLOAT) {
                throw new IllegalStateException("Invalid type 'float' for column 'last_recorded_mobile_received_bytes_value'");
            }
            if (!columnTypes.containsKey("last_recorded_mobile_transmitted_bytes_value")) {
                throw new IllegalStateException("Missing column 'last_recorded_mobile_transmitted_bytes_value'");
            }
            if (columnTypes.get("last_recorded_mobile_transmitted_bytes_value") != ColumnType.FLOAT) {
                throw new IllegalStateException("Invalid type 'float' for column 'last_recorded_mobile_transmitted_bytes_value'");
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
        return Arrays.asList("last_recorded_all_received_bytes_value", "last_recorded_all_transmitted_bytes_value", "last_recorded_mobile_received_bytes_value", "last_recorded_mobile_transmitted_bytes_value", "datetime");
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder("DataUsageState = [");
        stringBuilder.append("{last_recorded_all_received_bytes_value:");
        stringBuilder.append(getLast_recorded_all_received_bytes_value());
        stringBuilder.append("}");
        stringBuilder.append(",");
        stringBuilder.append("{last_recorded_all_transmitted_bytes_value:");
        stringBuilder.append(getLast_recorded_all_transmitted_bytes_value());
        stringBuilder.append("}");
        stringBuilder.append(",");
        stringBuilder.append("{last_recorded_mobile_received_bytes_value:");
        stringBuilder.append(getLast_recorded_mobile_received_bytes_value());
        stringBuilder.append("}");
        stringBuilder.append(",");
        stringBuilder.append("{last_recorded_mobile_transmitted_bytes_value:");
        stringBuilder.append(getLast_recorded_mobile_transmitted_bytes_value());
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
        DataUsageStateRealmProxy aDataUsageState = (DataUsageStateRealmProxy)o;

        String path = realm.getPath();
        String otherPath = aDataUsageState.realm.getPath();
        if (path != null ? !path.equals(otherPath) : otherPath != null) return false;;

        String tableName = row.getTable().getName();
        String otherTableName = aDataUsageState.row.getTable().getName();
        if (tableName != null ? !tableName.equals(otherTableName) : otherTableName != null) return false;

        return true;
    }

}
