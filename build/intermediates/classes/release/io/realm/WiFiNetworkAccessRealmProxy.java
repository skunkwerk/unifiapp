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

public class WiFiNetworkAccessRealmProxy extends WiFiNetworkAccess {

    @Override
    public String getSsid() {
        realm.checkIfValid();
        return (java.lang.String) row.getString(Realm.columnIndices.get("WiFiNetworkAccess").get("ssid"));
    }

    @Override
    public void setSsid(String value) {
        realm.checkIfValid();
        row.setString(Realm.columnIndices.get("WiFiNetworkAccess").get("ssid"), (String) value);
    }

    @Override
    public String getMac_address() {
        realm.checkIfValid();
        return (java.lang.String) row.getString(Realm.columnIndices.get("WiFiNetworkAccess").get("mac_address"));
    }

    @Override
    public void setMac_address(String value) {
        realm.checkIfValid();
        row.setString(Realm.columnIndices.get("WiFiNetworkAccess").get("mac_address"), (String) value);
    }

    @Override
    public String getEncrypted_password() {
        realm.checkIfValid();
        return (java.lang.String) row.getString(Realm.columnIndices.get("WiFiNetworkAccess").get("encrypted_password"));
    }

    @Override
    public void setEncrypted_password(String value) {
        realm.checkIfValid();
        row.setString(Realm.columnIndices.get("WiFiNetworkAccess").get("encrypted_password"), (String) value);
    }

    @Override
    public String getAuthentication_algorithm() {
        realm.checkIfValid();
        return (java.lang.String) row.getString(Realm.columnIndices.get("WiFiNetworkAccess").get("authentication_algorithm"));
    }

    @Override
    public void setAuthentication_algorithm(String value) {
        realm.checkIfValid();
        row.setString(Realm.columnIndices.get("WiFiNetworkAccess").get("authentication_algorithm"), (String) value);
    }

    public static Table initTable(ImplicitTransaction transaction) {
        if(!transaction.hasTable("class_WiFiNetworkAccess")) {
            Table table = transaction.getTable("class_WiFiNetworkAccess");
            table.addColumn(ColumnType.STRING, "ssid");
            table.addColumn(ColumnType.STRING, "mac_address");
            table.addColumn(ColumnType.STRING, "encrypted_password");
            table.addColumn(ColumnType.STRING, "authentication_algorithm");
            return table;
        }
        return transaction.getTable("class_WiFiNetworkAccess");
    }

    public static void validateTable(ImplicitTransaction transaction) {
        if(transaction.hasTable("class_WiFiNetworkAccess")) {
            Table table = transaction.getTable("class_WiFiNetworkAccess");
            if(table.getColumnCount() != 4) {
                throw new IllegalStateException("Column count does not match");
            }
            Map<String, ColumnType> columnTypes = new HashMap<String, ColumnType>();
            for(long i = 0; i < 4; i++) {
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
            if (!columnTypes.containsKey("encrypted_password")) {
                throw new IllegalStateException("Missing column 'encrypted_password'");
            }
            if (columnTypes.get("encrypted_password") != ColumnType.STRING) {
                throw new IllegalStateException("Invalid type 'String' for column 'encrypted_password'");
            }
            if (!columnTypes.containsKey("authentication_algorithm")) {
                throw new IllegalStateException("Missing column 'authentication_algorithm'");
            }
            if (columnTypes.get("authentication_algorithm") != ColumnType.STRING) {
                throw new IllegalStateException("Invalid type 'String' for column 'authentication_algorithm'");
            }
        }
    }

    public static List<String> getFieldNames() {
        return Arrays.asList("ssid", "mac_address", "encrypted_password", "authentication_algorithm");
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder("WiFiNetworkAccess = [");
        stringBuilder.append("{ssid:");
        stringBuilder.append(getSsid());
        stringBuilder.append("}");
        stringBuilder.append(",");
        stringBuilder.append("{mac_address:");
        stringBuilder.append(getMac_address());
        stringBuilder.append("}");
        stringBuilder.append(",");
        stringBuilder.append("{encrypted_password:");
        stringBuilder.append(getEncrypted_password());
        stringBuilder.append("}");
        stringBuilder.append(",");
        stringBuilder.append("{authentication_algorithm:");
        stringBuilder.append(getAuthentication_algorithm());
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
        WiFiNetworkAccessRealmProxy aWiFiNetworkAccess = (WiFiNetworkAccessRealmProxy)o;

        String path = realm.getPath();
        String otherPath = aWiFiNetworkAccess.realm.getPath();
        if (path != null ? !path.equals(otherPath) : otherPath != null) return false;;

        String tableName = row.getTable().getName();
        String otherTableName = aWiFiNetworkAccess.row.getTable().getName();
        if (tableName != null ? !tableName.equals(otherTableName) : otherTableName != null) return false;

        return true;
    }

}
