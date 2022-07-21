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

public class WiFiNetworkRealmProxy extends WiFiNetwork {

    @Override
    public String getSsid() {
        realm.checkIfValid();
        return (java.lang.String) row.getString(Realm.columnIndices.get("WiFiNetwork").get("ssid"));
    }

    @Override
    public void setSsid(String value) {
        realm.checkIfValid();
        row.setString(Realm.columnIndices.get("WiFiNetwork").get("ssid"), (String) value);
    }

    @Override
    public String getMac_address() {
        realm.checkIfValid();
        return (java.lang.String) row.getString(Realm.columnIndices.get("WiFiNetwork").get("mac_address"));
    }

    @Override
    public void setMac_address(String value) {
        realm.checkIfValid();
        row.setString(Realm.columnIndices.get("WiFiNetwork").get("mac_address"), (String) value);
    }

    @Override
    public int getFrequency() {
        realm.checkIfValid();
        return (int) row.getLong(Realm.columnIndices.get("WiFiNetwork").get("frequency"));
    }

    @Override
    public void setFrequency(int value) {
        realm.checkIfValid();
        row.setLong(Realm.columnIndices.get("WiFiNetwork").get("frequency"), (long) value);
    }

    @Override
    public float getLatitude() {
        realm.checkIfValid();
        return (float) row.getFloat(Realm.columnIndices.get("WiFiNetwork").get("latitude"));
    }

    @Override
    public void setLatitude(float value) {
        realm.checkIfValid();
        row.setFloat(Realm.columnIndices.get("WiFiNetwork").get("latitude"), (float) value);
    }

    @Override
    public float getLongitude() {
        realm.checkIfValid();
        return (float) row.getFloat(Realm.columnIndices.get("WiFiNetwork").get("longitude"));
    }

    @Override
    public void setLongitude(float value) {
        realm.checkIfValid();
        row.setFloat(Realm.columnIndices.get("WiFiNetwork").get("longitude"), (float) value);
    }

    @Override
    public float getAltitude() {
        realm.checkIfValid();
        return (float) row.getFloat(Realm.columnIndices.get("WiFiNetwork").get("altitude"));
    }

    @Override
    public void setAltitude(float value) {
        realm.checkIfValid();
        row.setFloat(Realm.columnIndices.get("WiFiNetwork").get("altitude"), (float) value);
    }

    public static Table initTable(ImplicitTransaction transaction) {
        if(!transaction.hasTable("class_WiFiNetwork")) {
            Table table = transaction.getTable("class_WiFiNetwork");
            table.addColumn(ColumnType.STRING, "ssid");
            table.addColumn(ColumnType.STRING, "mac_address");
            table.addColumn(ColumnType.INTEGER, "frequency");
            table.addColumn(ColumnType.FLOAT, "latitude");
            table.addColumn(ColumnType.FLOAT, "longitude");
            table.addColumn(ColumnType.FLOAT, "altitude");
            return table;
        }
        return transaction.getTable("class_WiFiNetwork");
    }

    public static void validateTable(ImplicitTransaction transaction) {
        if(transaction.hasTable("class_WiFiNetwork")) {
            Table table = transaction.getTable("class_WiFiNetwork");
            if(table.getColumnCount() != 6) {
                throw new IllegalStateException("Column count does not match");
            }
            Map<String, ColumnType> columnTypes = new HashMap<String, ColumnType>();
            for(long i = 0; i < 6; i++) {
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
        }
    }

    public static List<String> getFieldNames() {
        return Arrays.asList("ssid", "mac_address", "frequency", "latitude", "longitude", "altitude");
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder("WiFiNetwork = [");
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
        WiFiNetworkRealmProxy aWiFiNetwork = (WiFiNetworkRealmProxy)o;

        String path = realm.getPath();
        String otherPath = aWiFiNetwork.realm.getPath();
        if (path != null ? !path.equals(otherPath) : otherPath != null) return false;;

        String tableName = row.getTable().getName();
        String otherTableName = aWiFiNetwork.row.getTable().getName();
        if (tableName != null ? !tableName.equals(otherTableName) : otherTableName != null) return false;

        return true;
    }

}