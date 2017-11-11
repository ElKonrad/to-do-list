package pl.pollub.service.impl

import com.google.common.collect.Sets
import org.modelmapper.ModelMapper
import pl.pollub.component.CustomMapper
import pl.pollub.component.CustomMapperImpl
import pl.pollub.domain.Task
import pl.pollub.domain.User
import pl.pollub.dto.NewTask
import pl.pollub.exception.TaskForUserNotFoundException
import pl.pollub.exception.TaskNotFoundException
import pl.pollub.repository.InMemoryTaskRepository
import pl.pollub.service.TaskService
import pl.pollub.service.UserService
import spock.lang.Specification
import spock.lang.Unroll

import java.util.stream.Collectors

/**
 * Created by konrad on 11.11.17.
 */
class TaskServiceImplTest extends Specification {

    UserService userService = Mock()
    CustomMapper customMapper
    TaskService taskService

    def setup() {
        customMapper = new CustomMapperImpl(new ModelMapper())
        taskService = new TaskServiceImpl(new InMemoryTaskRepository(), userService)
    }

    def "Test custom mapper from DTO to entity cllas"() {
        given: "Task DTO is created"
        NewTask newTask = new NewTask(content)

        when: "Task DTO is mapped to entity class"
        Task task = customMapper.mapToEntity(newTask)

        then:
        task.getContent().equals(newTask.getContent())
        newTask.getContent().length() == contentLenght

//        to create table with only one parameter(column), must use _ as empty next column
        where:
        content                  | _
        "Testing SpockFramework" | _

        contentLenght = content.length() // for each "content" parameter
    }

    @Unroll
    def "when create new task and get task list then this tasks is on the task list with content: #content#number"() {
        given:
        Task createdTask1 = taskService.saveTask(customMapper.mapToEntity(new NewTask(content + number)))
        Task createdTask2 = taskService.saveTask(customMapper.mapToEntity(new NewTask(content + number)))

        when:
        List<Task> taskList = taskService.getAllTasks()

        then:
        taskList.size() == 2
        taskList.contains(createdTask1)
        taskList.contains(createdTask2)

        where:
        [content, number] << [["Task", "1"], ["Task", "2"]]
    }

    @Unroll
    def "can remove existing task by id with content #content#number"() {
        given:
        Task createdTask1 = taskService.saveTask(customMapper.mapToEntity(new NewTask(content + number)))
        Task createdTask2 = taskService.saveTask(customMapper.mapToEntity(new NewTask(content + number)))

        when: "remove existing first task"
        taskService.deleteTaskById(createdTask1.getId())
        List<Task> taskList = taskService.getAllTasks()

        then: "task list has one element, it's createdTask2"
        taskList.size() == 1
        taskList.contains(createdTask2)

        where:
        content | number
        "Task"  | "1"
        "Task"  | "2"
    }

    def "should throw an exception when get by id nonexistent task"() {
        when:
        taskService.getTaskById(1L)
        then:
        thrown(TaskNotFoundException)
    }

    def "when create task with owner, then can find this task by owner id"() {
        given:
        User owner = new User(1L, "user1")
        Task task = new Task(1L, "Test1", true, owner, null)

        when:
        taskService.saveTask(task)
        userService.getUserById(_) >> owner
        Set<Task> ownerTasks = taskService.getTasksByOwnerId(owner.getId())

        then:
        ownerTasks.contains(task)
        1 * userService.getUserById(_)
    }

    def "should throw exception when find task by owner id and this user is not owner any task"() {
        given:
        User owner = new User(1L, "user1")
        User notOwner = new User(2L, "user2")
        Task task = new Task(1L, "Test1", true, owner, null)

        when:
        taskService.saveTask(task)
        userService.getUserById(_) >> notOwner
        taskService.getTasksByOwnerId(notOwner.getId())

        then:
        thrown(TaskForUserNotFoundException)
    }

    def "when update task by adding contributors then contributors share this task"() {
        given:
        User owner = new User(1L, "user1")
        User contributor1 = new User(2L, "user2")
        User contributor2 = new User(3L, "user3")
        Task task = new Task(1L, "Test1", true, owner, null)

        when:
        taskService.saveTask(task)
        NewTask newTask = new NewTask("UpdatedContent", Sets.newHashSet(new User(null, "user2"), new User(null, "user3")))
        Task newTaskEntity = customMapper.mapToEntity(newTask)
        newTaskEntity.setId(task.getId())
        userService.getUserByUsername(contributor1.getUsername()) >> contributor1
        userService.getUserByUsername(contributor2.getUsername()) >> contributor2
        Task updatedTask = taskService.updateTask(newTaskEntity)
        Set<User> contributors = updatedTask.getContributors().stream().collect(Collectors.toSet())

        then:
        contributors.contains(contributor1)
        contributors.contains(contributor2)
    }
}
