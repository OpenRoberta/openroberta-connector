package de.fhg.iais.roberta.ui;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.ListResourceBundle;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class UiTests {

    @Test
    void showPopup_ShouldThrowIllegalArgument_WhenGivenWrongAmountOfEntries() {
        ListResourceBundle messages = new ListResourceBundle() {
            @Override
            protected Object[][] getContents() {
                Map<String, String> map = new HashMap<>();
                map.put("test", "test");
                map.put("0", "no placeholder, one supplied");
                map.put("1", "one placeholder, too few supplied {0}");
                map.put("2", "one placeholder, too many supplied {0}");


                String[][] array = new String[map.size()][2];
                int count = 0;
                for(Map.Entry<String,String> entry : map.entrySet()){
                    array[count][0] = entry.getKey();
                    array[count][1] = entry.getValue();
                    count++;
                }
                return array;
            }
        };

        assertThrows(IllegalArgumentException.class, () -> {
            OraPopup.showPopup(null, "test", "0", messages, null, new String[]{ "test"}, "Test0");
        });
        assertThrows(IllegalArgumentException.class, () -> {
            OraPopup.showPopup(null, "test", "1", messages, null, new String[]{ "test"});
        });
        assertThrows(IllegalArgumentException.class, () -> {
            OraPopup.showPopup(null, "test", "2", messages, null, new String[]{ "test"}, "Test0", "Test1");
        });
    }
}
