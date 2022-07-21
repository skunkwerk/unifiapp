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

public class AddedWiFiNetworkRealmProxy extends AddedWiFiNetwork {

    @Override
    public String getSsid() {
        realm.checkIfValid();
        return (java.lang.String) row.getString(Realm.columnIndices.get("AddedWiFiNetwork").get("ssid"));
    }

    @Override
    public void setSsid(String value) {
        realm.checkIfValid();
        row.setString(Realm.columnIndices.get("AddedWiFiNetwork").get("ssid"), (String) value);
    }

    @Override
    public String getMac_address() {
        realm.checkIfValid();
        return (java.lang.String) row.getString(Realm.columnIndices.get("AddedWiFiNetwork").get("mac_address"));
    }

    @Override
    public void setMac_address(String value) {
        realm.checkIfValid();
        row.setString(Realm.columnIndices.get("AddedWiFiNetwork").get("mac_address"), (String) value);
    }

    @Override
    public int getFrequency() {
        realm.checkIfValid();
        return (int) row.getLong(Realm.columnIndices.get("AddedWiFiNetwork").get("frequency"));
    }

    @Override
    public void setFrequency(int value) {
        realm.checkIfValid();
        row.setLong(Realm.columnIndices.get("AddedWiFiNetwork").get("frequency"), (long) value);
    }

    @Override
    public float getLatitude() {
        realm.checkIfValid();
        return (float) row.getFloat(Realm.columnIndices.get("AddedWiFiNetwork").get("latitude"));
    }

    @Override
    public void setLatitude(float value) {
        realm.checkIfValid();
        row.setFloat(Realm.columnIndices.get("AddedWiFiNetwork").get("latitude"), (float) value);
    }

    @Override
    public float getLongitude() {
        realm.checkIfValid();
        return (float) row.getFloat(Realm.columnIndices.get("AddedWiFiNetwork").get("longitude"));
    }

    @Override
    public void setLongitude(float value) {
        realm.checkIfValid();
        row.setFloat(Realm.columnIndices.get("AddedWiFiNetwork").get("longitude"), (float) value);
    }

    @Override
    public float getAltitude() {
        realm.checkIfValid();
        return (float) row.getFloat(Realm.columnIndices.get("AddedWiFiNetwork").get("altitude"));
    }

    @Override
    public void setAltitude(float value) {
        realm.checkIfValid();
        row.setFloat(Realm.columnIndices.get("AddedWiFiNetwork").get("altitude"), (float) value);
    }

    @Override
    public String getPassword() {
        realm.checkIfValid();
        return (java.lang.String) row.getString(Realm.columnIndices.get("AddedWiFiNetwork").get("password"));
    }

    @Override
    public void setPassword(String value) {
        realm.checkIfValid();
        row.setString(Realm.columnIndices.get("AddedWiFiNetwork").get("password"), (String) value);
    }

    @Override
    public long getLast_password_check_date() {
        realm.checkIfValid();
        return (long) row.getLong(Realm.columnIndices.get("AddedWiFiNetwork").get("last_password_check_date"));
    }

    @Override
    public void setLast_password_check_date(long value) {
        realm.checkIfValid();
        row.setLong(Realm.columnIndices.get("AddedWiFiNetwork").get("last_password_check_date"), (long) value);
    }

    public static Table initTable(ImplicitTransaction transaction) {
        if(!transaction.hasTable("class_AddedWiFiNetwork")) {
            Table table = transaction.getTable("class_AddedWiFiNetwork");
            table.addColumn(ColumnType.STRING, "ssid");
            table.addColumn(ColumnType.STRING, "mac_address");
            table.addColumn(ColumnType.INTEGER, "frequency");
            table.addColumn(ColumnType.FLOAT, "latitude");
            table.addColumn(ColumnType.FLOAT, "longitude");
            table.addColumn(ColumnType.FLOAT, "altitude");
            table.addColumn(ColumnType.STRING, "password");
            table.addColumn(ColumnType.INTEGER, "last_password_check_date");
            return table;
        }
        return transaction.getTable("class_AddedWiFiNetwork");
    }

    public static void validateTable(ImplicitTransaction transaction) {
        if(transaction.hasTable("class_AddedWiFiNetwork")) {
            Table table = transaction.getTable("class_AddedWiFiNetwork");
            if(table.getColumnCount() != 8) {
                throw new IllegalStateException("Column count does not match");
            }
            Map<String, ColumnType> columnTypes = new HashMap<String, ColumnType>();
            for(long i = 0; i < 8; i++) {
                columnTypes.put(table.getColumnName(i), table.getColumnType(i));
            }
            if (!columnTypes.containsKey("ssid")) {
                throw new IllegalStateException("Missing column 'ssid'");
            }
            if (columnTypes.get("ssid") != ColumnType.STRING) {
                throw new IllegalStateException("Invalid type 'String' for column 'ssid'");
            }
            if (!columnTypes.containsKey("mac_address")) {
                throw new IllegalStateException("Missing column 'mac_address'");
            }
            if (columnTypes.get("mac_address") != ColumnType.STRING) {
                throw new IllegalStateException("Invalid type 'String' for column 'mac_address'");
            }
            if (!columnTypes.containsKey("frequency")) {
                throw new IllegalStateException("Missing column 'frequency'");
            }
            if (columnTypes.get("frequency") != ColumnType.INTEGER) {
                throw new IllegalStateException("Invalid type 'int' for column 'frequency'");
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
            if (!columnTypes.containsKey("password")) {
                throw new IllegalStateException("Missing column 'password'");
            }
            if (columnTypes.get("password") != ColumnType.STRING) {
                throw new IllegalStateException("Invalid type 'String' for column 'password'");
            }
            if (!columnTypes.containsKey("last_password_check_date")) {
                throw new IllegalStateException("Missing column 'last_password_check_date'");
            }
            if (columnTypes.get("last_password_check_date") != ColumnType.INTEGER) {
                throw new IllegalStateException("Invalid type 'long' for column 'last_password_check_date'");
            }
        }
    }

    public static List<String> getFieldNames() {
        return Arrays.asList("ssid", "mac_address", "frequency", "latitude", "longitude", "altitude", "password", "last_password_check_date");
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder("AddedWiFiNetwork = [");
        stringBuilder.append("{ssid:");
        stringBuilder.append(getSsid());
        stringBuilder.append("}");
        stringBuilder.append(",");
        stringBuilder.append("{mac_address:");
        stringBuilder.append(getMac_address());
        stringBuilder.append("}");
        stringBuilder.append(",");
        stringBuilder.append("{frequency:");
        stringBuilder.append(getFrequency());
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
        stringBuilder.append("{password:");
        stringBuilder.append(getPassword());
        stringBuilder.append("}");
        stringBuilder.append(",");
        stringBuilder.append("{last_password_check_date:");
        stringBuilder.append(getLast_password_check_date());
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
        AddedWiFiNetworkRealmProxy aAddedWiFiNetwork = (AddedWiFiNetworkRealmProxy)o;

        String path = realm.getPath();
        String otherPath = aAddedWiFiNetwork.realm.getPath();
        if (path != null ? !path.equals(otherPath) : otherPath != null) return false;;

        String tableName = row.getTable().getName();
        String otherTableName = aAddedWiFiNetwork.row.getTable().getName();
        if (tableName != null ? !tableName.equals(otherTableName) : otherTableName != null) return false;

        return true;
    }

}
