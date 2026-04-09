import { Directive, Input, TemplateRef, ViewContainerRef, OnDestroy } from '@angular/core';
import { Subscription } from 'rxjs';
import { AuthService } from '../services/auth.service';

@Directive({
  selector: '[appHasRole]',
  standalone: true
})
export class HasRoleDirective implements OnDestroy {
  private hasView = false;
  private roles: string[] | undefined;
  private sub?: Subscription;

  constructor(
    private templateRef: TemplateRef<unknown>,
    private viewContainer: ViewContainerRef,
    private authService: AuthService
  ) {
    this.sub = this.authService.currentUser$.subscribe(() => this.updateView());
  }

  @Input() set appHasRole(roles: string[] | undefined) {
    this.roles = roles;
    this.updateView();
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
  }

  private updateView(): void {
    const user = this.authService.getCurrentUser();
    const userRole = user?.role;
    const canShow = !!(userRole && this.roles && this.roles.includes(userRole));

    if (canShow && !this.hasView) {
      this.viewContainer.createEmbeddedView(this.templateRef);
      this.hasView = true;
    } else if (!canShow && this.hasView) {
      this.viewContainer.clear();
      this.hasView = false;
    }
  }
}
