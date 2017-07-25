package pl.pollub.component;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.pollub.domain.NewTask;
import pl.pollub.domain.Task;

/**
 * Created by konrad on 25.07.17.
 */
@Component
public class CustomMapperImpl implements CustomMapper {

    private ModelMapper modelMapper;

    @Autowired
    public CustomMapperImpl(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    @Override
    public Task mapToEntity(NewTask newTask) {
        return modelMapper.map(newTask, Task.class);
    }
}