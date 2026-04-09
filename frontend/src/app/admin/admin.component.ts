import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { UserService } from '../services/user.service';
import { User } from '../models/user.model';
import { ToastService } from '../services/toast.service';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin.component.html'
})
export class AdminComponent implements OnInit {
  users: User[] = [];
  isLoading = false;
  roles: string[] = ['ALL', 'ADMIN', 'MANAGER', 'MEMBER', 'VIEWER', 'INACTIVE'];
  searchTerm = '';
  selectedRole = 'ALL';

  constructor(
    private userService: UserService,
    private toastService: ToastService
  ) { }

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.isLoading = true;
    this.userService.getAdminUsers().subscribe({
      next: (data) => {
        this.users = data.map(u => ({
          ...u,
          isActive: (u as any).active ?? u.isActive // Fix mapping from backend 'active' to frontend 'isActive'
        }));
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
        this.toastService.error('Failed to load users.');
      }
    });
  }

  get totalAdmins(): number {
    return this.users.filter(u => u.role === 'ADMIN').length;
  }

  get totalManagers(): number {
    return this.users.filter(u => u.role === 'MANAGER').length;
  }

  get totalInactive(): number {
    return this.users.filter(u => u.isActive === false).length;
  }

  get filteredUsers(): User[] {
    let users = [...this.users];

    if (this.selectedRole && this.selectedRole !== 'ALL') {
      if (this.selectedRole === 'INACTIVE') {
        users = users.filter(u => u.isActive === false);
      } else {
        users = users.filter(u => u.role === this.selectedRole);
      }
    }

    if (this.searchTerm.trim()) {
      const term = this.searchTerm.trim().toLowerCase();
      users = users.filter(u =>
        u.username.toLowerCase().includes(term) ||
        u.email.toLowerCase().includes(term)
      );
    }

    return users;
  }

  setFilterRole(role: string): void {
    this.selectedRole = role;
  }

  onRoleChange(user: User, role: string): void {
    if (!user.id) return;
    this.userService.changeUserRole(user.id, role).subscribe({
      next: () => {
        user.role = role;
        this.toastService.success('User role updated.');
        this.loadUsers();
      },
      error: () => {
        this.toastService.error('Failed to update role.');
      }
    });
  }

  toggleActive(user: User): void {
    if (!user.id) return;
    const target = !user.isActive;
    this.userService.setUserActive(user.id, target).subscribe({
      next: () => {
        this.toastService.success(`User ${target ? 'activated' : 'deactivated'}.`);
        this.loadUsers();
      },
      error: () => {
        this.toastService.error('Failed to update user status.');
      }
    });
  }

  getRoleBadgeClass(role?: string): string {
    switch ((role || '').toUpperCase()) {
      case 'ADMIN':
        return 'tf-role-badge tf-role-badge-admin';
      case 'MANAGER':
        return 'tf-role-badge tf-role-badge-manager';
      case 'LEAD':
      case 'TEAM_LEAD':
        return 'tf-role-badge tf-role-badge-lead';
      default:
        return 'tf-role-badge tf-role-badge-member';
    }
  }

  getInitials(name: string): string {
    if (!name) return '?';
    return name.split(' ').map(n => n[0]).join('').toUpperCase().substring(0, 2);
  }

  editUser(user: User): void {
    // Placeholder for future modal-based editing
    this.toastService.info(`Editing ${user.username} (Coming Soon)`);
  }

  deleteUser(user: User): void {
    if (!user.id) return;
    this.toastService.confirm(`Delete user ${user.username}? This cannot be undone.`, () => {
      this.userService.deleteUser(user.id!).subscribe({
        next: () => {
          this.toastService.success('User deleted.');
          this.loadUsers();
        },
        error: () => this.toastService.error('Failed to delete user.')
      });
    });
  }
}
