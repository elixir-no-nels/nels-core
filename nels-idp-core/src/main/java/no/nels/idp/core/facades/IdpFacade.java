package no.nels.idp.core.facades;

import no.nels.commons.model.NumberIndexedList;
import no.nels.idp.core.Config;
import no.nels.idp.core.model.db.NeLSIdpUser;
import no.nels.idp.core.model.db.DBIdpMapper;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import java.util.*;

/**
 * Created by Kidane on 22.05.2015.
 */
public class IdpFacade {
    public static long getIdpUsersCount() {
        return Config.getIdpDBHelper().getCount("idp");
    }

    public static NumberIndexedList getAllIdpUsers() {
        NumberIndexedList ret = new NumberIndexedList();
        String cmd = "select * from idp order by first_name, last_name ";
        List<NeLSIdpUser> idps = Config.getIdpDBHelper().executeQuery(cmd, new DBIdpMapper());
        for (NeLSIdpUser idp : idps) {
            ret.add(idp);
        }
        return ret;
    }

    public static NeLSIdpUser getById(long id) {
        return (NeLSIdpUser) Config.getIdpDBHelper().getById("idp", id, new DBIdpMapper());
    }

    public static NeLSIdpUser getByUserName(String username) {
        return (NeLSIdpUser) Config.getIdpDBHelper().getByColumn("idp", "username", username, new DBIdpMapper());
    }

    public static NeLSIdpUser getByEmail(String email) {
        return (NeLSIdpUser) Config.getIdpDBHelper().getByColumn("idp", "email", email, new DBIdpMapper());
    }

    public static NeLSIdpUser RegisterUser(String username, String plainPwd, String firstName, String lastName, String email, String affiliation) {
        String cmd = "INSERT INTO idp (username,password,creationdate,isactive,first_name,last_name,email,affiliation) VALUES(?,?,?,?,?,?,?,?)";
        try {
            if (Config.getIdpDBHelper().executeNonQuery(cmd,
                    new Object[]{username, cipherPassword(plainPwd),
                            new Date(), true, firstName, lastName, email, affiliation}
            )) {
                return getByUserName(username);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static NeLSIdpUser UpdateUser(long id, String username, String firstName, String lastName, String email, String affiliation) {
        String cmd = "update  idp set username =? ,first_name=?, last_name=?, email=?,affiliation=? where id=?";
        try {
            Config.getIdpDBHelper().executeNonQuery(cmd, new Object[]{username, firstName, lastName, email, affiliation, id});
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
        return getByUserName(username);
    }

    private static String byteToHex(final byte[] hash) {
        Formatter formatter = new Formatter();
        for (byte b : hash) {
            formatter.format("%02x", b);
        }
        String result = formatter.toString();
        formatter.close();
        return result;
    }

    public static String cipherPassword(String s) {
        try {
            return byteToHex(MessageDigest.getInstance("SHA-1").digest(s.getBytes("UTF-8"))).toUpperCase();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return s;
    }

    public static NeLSIdpUser UpdatePassword(long id, String password) {
        String cmd = "update  idp set password=? where id=?";
        try {
            Config.getIdpDBHelper().executeNonQuery(cmd, new Object[]{cipherPassword(password), id});
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
        return getById(id);
    }

}
