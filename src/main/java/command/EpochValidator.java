package command;

import com.beust.jcommander.IValueValidator;
import com.beust.jcommander.ParameterException;

import java.text.ParseException;

public class EpochValidator implements IValueValidator<String> {
    @Override
    public void validate(String name, String value) throws ParameterException {
        if (value.isEmpty()) {
            throw new ParameterException("No date has been given.");
        }

        try {
            CommandOptions.DATE_FORMAT.parse(value);
        } catch (ParseException e1) {
            throw new ParameterException("Invalid date format.");
        }
    }
}