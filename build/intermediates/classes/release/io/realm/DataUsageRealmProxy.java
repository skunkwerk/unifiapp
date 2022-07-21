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

public class DataUsageRealmProxy extends DataUsage {

    @Override
    public float getData_uploaded_mobile() {
        realm.checkIfValid();
        return (float) row.getFloat(Realm.columnIndices.get("DataUsage").get("data_uploaded_mobile"));
    }

    @Override
    public void setData_uploaded_mobile(float value) {
        realm.checkIfValid();
        row.setFloat(Realm.columnIndices.get("DataUsage").get("data_uploaded_mobile"), (float) value);
    }

    @Override
    public float getData_downloaded_mobile() {
        realm.checkIfValid();
        return (float) row.getFloat(Realm.columnIndices.get("DataUsage").get("data_downloaded_mobile"));
    }

    @Override
    public void setData_downloaded_mobile(float value) {
        realm.checkIfValid();
        row.setFloat(Realm.columnIndices.get("DataUsage").get("data_downloaded_mobile"), (float) value);
    }

    @Override
    public float getData_uploaded_wifi() {
        realm.checkIfValid();
        return (float) row.getFloat(Realm.columnIndices.get("DataUsage").get("data_uploaded_wifi"));
    }

    @Override
    public void setData_uploaded_wifi(float value) {
        realm.checkIfValid();
        row.setFloat(Realm.columnIndices.get("DataUsage").get("data_uploaded_wifi"), (float) value);
    }

    @Override
    public float getData_downloaded_wifi() {
        realm.checkIfValid();
        return (float) row.getFloat(Realm.columnIndices.get("DataUsage").get("data_downloaded_wifi"));
    }

    @Override
    public void setData_downloaded_wifi(float value) {
        realm.checkIfValid();
        row.setFloat(Realm.columnIndices.get("DataUsage").get("data_downloaded_wifi"), (float) value);
    }

    @Override
    public java.util.Date getDatetime() {
        realm.checkIfValid();
        return (java.util.Date) row.getDate(Realm.columnIndices.get("DataUsage").get("datetime"));
    }

    @Override
    public void setDatetime(java.util.Date value) {
        realm.checkIfValid();
        row.setDate(Realm.columnIndices.get("DataUsage").get("datetime"), (Date) value);
    }

    @Override
    public float getLatitude() {
        realm.checkIfValid();
        return (float) row.getFloat(Realm.columnIndices.get("DataUsage").get("latitude"));
    }

    @Override
    public void setLatitude(float value) {
        realm.checkIfValid();
        row.setFloat(Realm.columnIndices.get("DataUsage").get("latitude"), (float) value);
    }

    @Override
    public float getLongitude() {
        realm.checkIfValid();
        return (float) row.getFloat(Realm.columnIndices.get("DataUsage").get("longitude"));
    }

    @Override
    public void setLongitude(float value) {
        realm.checkIfValid();
        row.setFloat(Realm.columnIndices.get("DataUsage").get("longitude"), (float) value);
    }

    @Override
    public float getAltitude() {
        realm.checkIfValid();
        return (float) row.getFloat(Realm.columnIndices.get("DataUsage").get("altitude"));
    }

    @Override
    public void setAltitude(float value) {
        realm.checkIfValid();
        row.setFloat(Realm.columnIndices.get("DataUsage").get("altitude"), (float) value);
    }

    @Override
    public String getAnonymous_customer_id_hash() {
        realm.checkIfValid();
        return (java.lang.String) row.getString(Realm.columnIndices.get("DataUsage").get("anonymous_customer_id_hash"));
    }

    @Override
    public void setAnonymous_customer_id_hash(String value) {
        realm.checkIfValid();
        row.setString(Realm.columnIndices.get("DataUsage").get("anonymous_customer_id_hash"), (String) value);
    }

    public static Table initTable(ImplicitTransaction transaction) {
        if(!transaction.hasTable("class_DataUsage")) {
            Table table = transaction.getTable("class_DataUsage");
            table.addColumn(ColumnType.FLOAT, "data_uploaded_mobile");
            table.addColumn(ColumnType.FLOAT, "data_downloaded_mobile");
            table.addColumn(ColumnType.FLOAT, "data_uploaded_wifi");
            table.addColumn(ColumnType.FLOAT, "data_downloaded_wifi");
            table.addColumn(ColumnType.DATE, "datetime");
            table.addColumn(ColumnType.FLOAT, "latitude");
            table.addColumn(ColumnType.FLOAT, "longitude");
            table.addColumn(ColumnType.FLOAT, "altitude");
            table.addColumn(ColumnType.STRING, "anonymous_customer_id_hash");
            return table;
        }
        return transaction.getTable("class_DataUsage");
    }

    public static void validateTable(ImplicitTransaction transaction) {
        if(transaction.hasTable("class_DataUsage")) {
            Table table = transaction.getTable("class_DataUsage");
            if(table.getColumnCount() != 9) {
                throw new IllegalStateException("Column count does not match");
            }
            Map<String, ColumnType> columnTypes = new HashMap<String, ColumnType>();
            for(long i = 0; i < 9; i++) {
                columnTypes.put(table.getColumnName(i), table.getColumnType(i));
            }
            if (!columnTypes.containsKey("data_uploaded_mobile")) {
                throw new IllegalStateException("Missing column 'data_uploaded_mobile'");
            }
            if (columnTypes.get("data_uploaded_mobile") != ColumnType.FLOAT) {
                throw new IllegalStateException("Invalid type 'float' for column 'data_uploaded_mobile'");
            }
            if (!columnTypes.containsKey("data_downloaded_mobile")) {
                throw new IllegalStateException("Missing column 'data_downloaded_mobile'");
            }
            if (columnTypes.get("data_downloaded_mobile") != ColumnType.FLOAT) {
                throw new IllegalStateException("Invalid type 'float' for column 'data_downloaded_mobile'");
            }
            if (!columnTypes.containsKey("data_uploaded_wifi")) {
                throw new IllegalStateException("Missing column 'data_uploaded_wifi'");
            }
            if (columnTypes.get("data_uploaded_wifi") != ColumnType.FLOAT) {
                throw new IllegalStateException("Invalid type 'float' for column 'data_uploaded_wifi'");
            }
            if (!columnTypes.containsKey("data_downloaded_wifi")) {
                throw new IllegalStateException("Missing column 'data_downloaded_wifi'");
            }
            if (columnTypes.get("data_downloaded_wifi") != ColumnType.FLOAT) {
                throw new IllegalStateException("Invalid type 'float' for column 'data_downloaded_wifi'");
            }
            if (!columnTypes.containsKey("datetime")) {
                throw new IllegalStateException("Missing column 'datetime'");
            }
            if (columnTypes.get("datetime") != ColumnType.DATE) {
                throw new IllegalStateException("Invalid type 'Date' for column 'datetime'");
            }
            if (!columnTypes.containsKey("latitude")) {
                throw new IllegalStateException("Missing column 'latitude'");
            }
            if (columnTypes.get("latitude") != ColumnType.FLOAT) {
                throw new IllegalStateException("Invalid type 'float' for column 'latitude'");
            }
            if (!columnTypes.containsKey("longitude")) {
                throw new IllegalStateException("Missing column 'longitude'");
            }
            if (columnTypes.get("longitude") != ColumnType.FLOAT) {
                throw new IllegalStateException("Invalid type 'float' for column 'longitude'");
            }
            if (!columnTypes.containsKey("altitude")) {
                throw new IllegalStateException("Missing column 'altitude'");
            }
            if (columnTypes.get("altitude") != ColumnType.FLOAT) {
                throw new IllegalStateException("Invalid type 'float' for column 'altitude'");
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
        return Arrays.asList("data_uploaded_mobile", "data_downloaded_mobile", "data_uploaded_wifi", "data_downloaded_wifi", "datetime", "latitude", "longitude", "altitude", "anonymous_customer_id_hash");
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder("DataUsage = [");
        stringBuilder.append("{data_uploaded_mobile:");
        stringBuilder.append(getData_uploaded_mobile());
        stringBuilder.append("}");
        stringBuilder.append(",");
        stringBuilder.append("{data_downloaded_mobile:");
        stringBuilder.append(getData_downloaded_mobile());
        stringBuilder.append("}");
        stringBuilder.append(",");
        stringBuilder.append("{data_uploaded_wifi:");
        stringBuilder.append(getData_uploaded_wifi());
        stringBuilder.append("}");
        stringBuilder.append(",");
        stringBuilder.append("{data_downloaded_wifi:");
        stringBuilder.append(getData_downloaded_wifi());
        stringBuilder.append("}");
        stringBuilder.append(",");
        stringBuilder.append("{datetime:");
        stringBuilder.append(getDatetime());
        stringBuilder.append("}");
        stringBuilder.append(",");
        stringBuilder.append("{latitude:");
        stringBuilder.append(getLatitude());
        stringBuilder.append("}");
        stringBuilder.append(",");
        stringBuilder.append("{longitude:");
        stringBuilder.append(getLongitude());
        stringBuilder.append("}");
        stringBuilder.append(",");
        stringBuilder.append("{altitude:");
        stringBuilder.append(getAltitude());
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
        DataUsageRealmProxy aDataUsage = (DataUsageRealmProxy)o;

        String path = realm.getPath();
        String otherPath = aDataUsage.realm.getPath();
        if (path != null ? !path.equals(otherPath) : otherPath != null) return false;;

        String tableName = row.getTable().getName();
        String otherTableName = aDataUsage.row.getTable().getName();
        if (tableName != null ? !tableName.equals(otherTableName) : otherTableName != null) return false;

        return true;
    }

}
