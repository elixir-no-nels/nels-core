package no.nels.eup.core.facades;

import no.nels.eup.core.Config;
import no.nels.eup.core.model.db.SettingMapper;

import java.util.Date;

/**
 * Created by Kidane on 01.10.2015.
 */
public class SettingFacade {

    public  static boolean isSettingFound(String settingKey) {
        String cmd = "select count(*) from setting where setting_key=?";
        return Config.getEUPDBhelper().executeScalar(cmd, new Object[]{settingKey}) == 1;
    }

    public  static boolean isSettingFound(String settingKey, long itemId) {
        return  isSettingFound(getItemSettingKey(settingKey, itemId));
    }

    public static boolean isSettingMatch(String settingKey, long itemId, String value) {
        String cmd = "select count(*) from setting where setting_key=? and setting_value=?";
        return Config.getEUPDBhelper().executeScalar(cmd, new Object[]{getItemSettingKey(settingKey, itemId), value}) == 1;
        //return Config.getEUPDBhelper().executeQueryForSingleResult(cmd, new Object[] {getItemSettingKey(settingKey, itemId), value}, new SettingMapper()).getSettingValue();
    }

    public static boolean removeSetting(String settingKey){
        String cmd = "delete from setting where setting_key=?";
        return Config.getEUPDBhelper().executeNonQuery(cmd, new Object[]{settingKey});
    }

    public static boolean removeSetting(String settingKey, long itemId) {
        return removeSetting(getItemSettingKey(settingKey, itemId));
    }

    private  static String getItemSettingKey(String settingKey, long itemId) {
        return  settingKey + "-" + itemId;
    }

    public static boolean setSetting(String settingKey, String settingValue) {
        if(!isSettingFound(settingKey)){
            String cmd = "insert into setting (setting_key, setting_value, lastupdate) values (?, ?, ?)";
            return Config.getEUPDBhelper().executeNonQuery(cmd, new Object[] {settingKey, settingValue, new Date()});
        }else {
            String cmd = "update setting set setting_value=?, lastupdate=? where setting_key=?";
            return Config.getEUPDBhelper().executeNonQuery(cmd, new Object[]{settingValue, new Date(), settingKey});
        }
    }

    public static boolean setSetting(String settingKey, long itemId, String settingValue) {
        return setSetting(getItemSettingKey(settingKey, itemId), settingValue);
    }

    public static String getSetting(String settingKey) {
        String cmd = "select * from setting where setting_key=? limit 1";
        return Config.getEUPDBhelper().executeQueryForSingleResult(cmd, new Object[] {settingKey}, new SettingMapper()).getSettingValue();
    }

    public static String getSetting(String settingKey, long itemId) {
        return getSetting(getItemSettingKey(settingKey, itemId));
    }





}
