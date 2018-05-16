package no.norstore.storebioinfo.utils;

import no.norstore.storebioinfo.constants.SqlKeyword;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SqlUtils {
    public static List<String> appendOffset(String offset) {
        List<String> result = new ArrayList<>();
        if (StringUtils.isNumeric(offset)) {
            result.add(SqlKeyword.OFFSET);
            result.add(offset);
            return result;
        } else {
            throw new IllegalArgumentException("offset is invalid");
        }
    }


    public static List<String> appendLimit(String limit) {
        List<String> result = new ArrayList<>();
        if (StringUtils.isNumeric(limit)) {
            result.add(SqlKeyword.LIMIT);
            result.add(limit);
            return result;
        } else {
            throw new IllegalArgumentException("limit is invalid");
        }
    }




    public static List<String> appendSorting(String sort) {
        List<String> result = new ArrayList<>();

        if (sort.charAt(0) == '-') {
            result.add(SqlKeyword.DESC);
            sort = sort.substring(1); //remove - character
        } else {
            result.add(SqlKeyword.ASC);
        }

        result.add(sort);
        result.add(SqlKeyword.ORDER_BY);
        Collections.reverse(result);

        return result;

    }
}
