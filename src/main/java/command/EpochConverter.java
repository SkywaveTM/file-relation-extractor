package command;

import com.beust.jcommander.IStringConverter;
import java.text.ParseException;

public class EpochConverter implements IStringConverter<Long> {

    @Override
    public Long convert(String value) {
        try {
            return CommandOptions.DATE_FORMAT.parse(value).getTime();
        } catch (ParseException ignored) {
            throw new IllegalArgumentException("date format should be " + CommandOptions.DATE_STRING_FORMAT);
        }
    }
}
