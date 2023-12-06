package cn.ac.iie.pkcgroup.dws.utils;

import cn.ac.iie.pkcgroup.dws.comm.request.db.SelectedField;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;
import java.util.List;

@Component
public class SelectedFieldConverter implements Converter<String, SelectedField[]> {

    @Override
    public SelectedField[] convert(String s) {
        Gson gson = new Gson();
        Type myType = new TypeToken<List<SelectedField>>(){}.getType();
        List<SelectedField> list = gson.fromJson(s, myType);
        if (list == null) return null;
        SelectedField[] res = new SelectedField[list.size()];
        for (int i = 0; i < list.size(); i++) {
            res[i] = list.get(i);
        }
        return res;
    }
}
