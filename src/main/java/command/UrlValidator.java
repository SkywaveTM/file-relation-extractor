package command;

import com.beust.jcommander.IValueValidator;
import com.beust.jcommander.ParameterException;
import utils.Logger;

import java.util.List;

public class UrlValidator implements IValueValidator<List<String>> {

    @Override
    public void validate(String name, List<String> value) throws ParameterException {
        value.forEach(Logger.INSTANCE::info);

        if (value.size() != 1) {
            // fixme: not working...
//             throw new ParameterException("Exactly 1 URL should be given.");
        }
    }
}
