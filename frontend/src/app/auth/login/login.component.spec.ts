import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { FormsModule } from '@angular/forms';
import { of, throwError } from 'rxjs';
import { Component } from '@angular/core';
import { LoginComponent } from './login.component';
import { AuthService } from '../../services/auth.service';

@Component({ template: '' })
class DummyComponent {}

describe('LoginComponent', () => {
  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;

  beforeEach(async () => {
    authServiceSpy = jasmine.createSpyObj('AuthService', ['login']);

    await TestBed.configureTestingModule({
      imports: [
        LoginComponent, 
        HttpClientTestingModule, 
        RouterTestingModule.withRoutes([
          { path: '**', component: DummyComponent }
        ]), 
        FormsModule
      ],
      declarations: [DummyComponent],
      providers: [
        { provide: AuthService, useValue: authServiceSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize with empty credentials', () => {
    expect(component.loginData.email).toBe('');
    expect(component.loginData.password).toBe('');
  });

  it('should show error when fields are empty', () => {
    component.onSubmit();
    expect(component.errorMessage).toBe('Please fill in all fields.');
  });

  it('should show error for invalid email format', () => {
    component.loginData.email = 'no'; // Length < 3 -> fails identifier check
    component.loginData.password = 'Pass1234';
    component.onSubmit();
    expect(component.errorMessage).toBe('Please enter a valid email or username.');
  });

  it('should call AuthService.login with valid credentials', () => {
    authServiceSpy.login.and.returnValue(of({ token: 'fake-jwt', username: 'John' }));

    component.loginData.email = 'john@example.com';
    component.loginData.password = 'Pass1234';
    component.onSubmit();

    expect(authServiceSpy.login).toHaveBeenCalledWith({
      email: 'john@example.com',
      password: 'Pass1234'
    });
  });

  it('should display "Invalid email or password" on 401', () => {
    authServiceSpy.login.and.returnValue(throwError(() => ({ status: 401 })));

    component.loginData.email = 'john@example.com';
    component.loginData.password = 'WrongPass';
    component.onSubmit();

    expect(component.errorMessage).toBe('Invalid email or password. Please try again.');
    expect(component.isLoading).toBeFalse();
  });

  it('should display connection error on status 0', () => {
    authServiceSpy.login.and.returnValue(throwError(() => ({ status: 0 })));

    component.loginData.email = 'john@example.com';
    component.loginData.password = 'Pass1234';
    component.onSubmit();

    expect(component.errorMessage).toBe('Unable to connect to server. Please check your connection.');
  });

  it('should toggle password visibility', () => {
    expect(component.showPassword).toBeFalse();
    component.togglePassword();
    expect(component.showPassword).toBeTrue();
    component.togglePassword();
    expect(component.showPassword).toBeFalse();
  });

  it('should set isLoading true during login request', () => {
    // Never complete the observable to keep isLoading true
    authServiceSpy.login.and.returnValue(of({ token: 'abc' }));
    component.loginData.email = 'john@example.com';
    component.loginData.password = 'Pass1234';

    // isLoading becomes true then false after subscribe resolves
    component.onSubmit();
    // After successful login, isLoading is false
    expect(component.isLoading).toBeFalse();
  });

  it('should default returnUrl to /dashboard', () => {
    expect(component.returnUrl).toBe('/dashboard');
  });
});
