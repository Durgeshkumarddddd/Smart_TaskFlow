import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TaskService } from './task.service';
import { Task, TaskStatus, TaskPriority } from '../models/task.model';
import { environment } from '../../environments/environment';

describe('TaskService', () => {
  let service: TaskService;
  let httpMock: HttpTestingController;

  const TASK_API = `${environment.apiUrl}/tasks`;

  const mockTask: Task = {
    id: 1,
    title: 'Test Task',
    description: 'A description',
    status: TaskStatus.PENDING,
    priority: TaskPriority.MEDIUM,
    dueDate: '2026-03-01',
    userId: 1
  };

  const mockTasks: Task[] = [
    mockTask,
    { id: 2, title: 'Task 2', status: TaskStatus.IN_PROGRESS, priority: TaskPriority.HIGH, dueDate: '2026-03-05' },
    { id: 3, title: 'Task 3', status: TaskStatus.COMPLETED, priority: TaskPriority.LOW, dueDate: '2026-02-20' }
  ];

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [TaskService]
    });

    service = TestBed.inject(TaskService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  // --- GET all tasks ---
  it('should fetch all tasks via GET', () => {
    service.getAllTasks().subscribe(tasks => {
      expect(tasks.length).toBe(3);
      expect(tasks).toEqual(mockTasks);
    });

    const req = httpMock.expectOne(TASK_API);
    expect(req.request.method).toBe('GET');
    req.flush(mockTasks);
  });

  it('should return empty array when no tasks exist', () => {
    service.getAllTasks().subscribe(tasks => {
      expect(tasks.length).toBe(0);
    });

    httpMock.expectOne(TASK_API).flush([]);
  });

  // --- GET task by ID ---
  it('should fetch a single task by ID via GET', () => {
    service.getTaskById(1).subscribe(task => {
      expect(task).toEqual(mockTask);
      expect(task.title).toBe('Test Task');
    });

    const req = httpMock.expectOne(`${TASK_API}/1`);
    expect(req.request.method).toBe('GET');
    req.flush(mockTask);
  });

  // --- POST create task ---
  it('should create a task via POST', () => {
    const newTask: Task = {
      title: 'New Task',
      status: TaskStatus.PENDING,
      priority: TaskPriority.LOW,
      dueDate: '2026-04-01'
    };

    service.createTask(newTask).subscribe(task => {
      expect(task.id).toBe(4);
      expect(task.title).toBe('New Task');
    });

    const req = httpMock.expectOne(TASK_API);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(newTask);
    req.flush({ ...newTask, id: 4 });
  });

  // --- PUT update task ---
  it('should update a task via PUT', () => {
    const updatedTask: Task = { ...mockTask, title: 'Updated Title', status: TaskStatus.IN_PROGRESS };

    service.updateTask(1, updatedTask).subscribe(task => {
      expect(task.title).toBe('Updated Title');
      expect(task.status).toBe(TaskStatus.IN_PROGRESS);
    });

    const req = httpMock.expectOne(`${TASK_API}/1`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body.title).toBe('Updated Title');
    req.flush(updatedTask);
  });

  // --- DELETE task ---
  it('should delete a task via DELETE', () => {
    service.deleteTask(1).subscribe(resp => {
      expect(resp).toBeNull();
    });

    const req = httpMock.expectOne(`${TASK_API}/1`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });

  // --- Error handling ---
  it('should propagate HTTP errors on getAllTasks', () => {
    service.getAllTasks().subscribe({
      next: () => fail('Expected error'),
      error: (err) => {
        expect(err.status).toBe(500);
      }
    });

    httpMock.expectOne(TASK_API).flush('Server error', { status: 500, statusText: 'Internal Server Error' });
  });

  it('should propagate 404 on getTaskById', () => {
    service.getTaskById(999).subscribe({
      next: () => fail('Expected error'),
      error: (err) => {
        expect(err.status).toBe(404);
      }
    });

    httpMock.expectOne(`${TASK_API}/999`).flush('Not found', { status: 404, statusText: 'Not Found' });
  });
});
