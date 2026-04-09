import { Component, OnInit, ViewChild, ElementRef, AfterViewInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AnalyticsService } from '../services/analytics.service';
import { ToastService } from '../services/toast.service';
import { TaskSummary } from '../models/task.model';
import { Chart, registerables } from 'chart.js';

Chart.register(...registerables);

@Component({
  selector: 'app-analytics',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './analytics.component.html'
})
export class AnalyticsComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('statusChart') statusChartRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('priorityChart') priorityChartRef!: ElementRef<HTMLCanvasElement>;

  summary: TaskSummary | null = null;
  isLoading = false;
  isCollapsed = true;

  private statusChart: Chart | null = null;
  private priorityChart: Chart | null = null;
  private chartsInitialized = false;

  get dueTodayCount(): number {
    return this.summary?.pendingTasks ?? 0;
  }

  get thisWeekCount(): number {
    return this.summary?.inProgressTasks ?? 0;
  }

  constructor(
    private analyticsService: AnalyticsService,
    private toastService: ToastService
  ) {}

  ngOnInit(): void {}

  ngAfterViewInit(): void {}

  ngOnDestroy(): void {
    this.destroyCharts();
  }

  togglePanel(): void {
    this.isCollapsed = !this.isCollapsed;
    if (!this.isCollapsed && !this.summary) {
      this.loadSummary();
    }
  }

  loadSummary(): void {
    this.isLoading = true;
    this.analyticsService.getSummary().subscribe({
      next: (data) => {
        if (!data.statusCounts) {
          data.statusCounts = {
            'PENDING': (data as any).pendingTasks || 0,
            'IN_PROGRESS': (data as any).inProgressTasks || 0,
            'COMPLETED': (data as any).completedTasks || 0
          };
        }
        if (!data.priorityCounts) {
          data.priorityCounts = {
            'HIGH': (data as any).highPriorityTasks || 0,
            'MEDIUM': (data as any).mediumPriorityTasks || 0,
            'LOW': (data as any).lowPriorityTasks || 0
          };
        }
        this.summary = data;
        this.isLoading = false;
        setTimeout(() => this.renderCharts(), 150);
      },
      error: () => {
        this.isLoading = false;
        this.toastService.error('Failed to load analytics data.');
      }
    });
  }

  refreshData(): void {
    this.destroyCharts();
    this.loadSummary();
  }

  private renderCharts(): void {
    if (!this.summary) return;
    this.destroyCharts();
    this.renderStatusChart();
    this.renderPriorityChart();
    this.chartsInitialized = true;
  }

  private renderStatusChart(): void {
    if (!this.statusChartRef) return;
    const ctx = this.statusChartRef.nativeElement.getContext('2d');
    if (!ctx) return;

    const sc = this.summary!.statusCounts || {};
    const labels = Object.keys(sc).map(k => this.formatLabel(k));
    const values = Object.values(sc);
    const colors = Object.keys(sc).map(k => this.getStatusColor(k));

    const isMobile = window.innerWidth < 768;

    this.statusChart = new Chart(ctx, {
      type: 'doughnut',
      data: {
        labels,
        datasets: [{
          data: values,
          backgroundColor: colors,
          borderWidth: 0,
          hoverOffset: 8
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            position: isMobile ? 'bottom' : 'right',
            labels: {
              color: this.getTextColor(),
              padding: isMobile ? 8 : 12,
              usePointStyle: true,
              pointStyleWidth: 10,
              font: { size: 10 }
            }
          }
        },
        cutout: '60%'
      },
      plugins: [{
        id: 'centerText',
        afterDraw: (chart: any) => {
          const { ctx: c, chartArea } = chart;
          const total = values.reduce((a: number, b: number) => a + b, 0);
          c.save();
          c.font = 'bold 24px Inter, sans-serif';
          c.fillStyle = this.getTextColor();
          c.textAlign = 'center';
          c.textBaseline = 'middle';
          const centerX = (chartArea.left + chartArea.right) / 2;
          const centerY = (chartArea.top + chartArea.bottom) / 2;
          c.fillText(total.toString(), centerX, centerY);
          c.restore();
        }
      }]
    });
  }

  private renderPriorityChart(): void {
    if (!this.priorityChartRef) return;
    const ctx = this.priorityChartRef.nativeElement.getContext('2d');
    if (!ctx) return;

    const pc = this.summary!.priorityCounts || {};
    const labels = Object.keys(pc).map(k => this.formatLabel(k));
    const values = Object.values(pc);
    const colors = Object.keys(pc).map(k => this.getPriorityColor(k));

    this.priorityChart = new Chart(ctx, {
      type: 'bar',
      data: {
        labels,
        datasets: [{
          label: 'Tasks',
          data: values,
          backgroundColor: colors,
          borderRadius: 6,
          borderSkipped: false,
          barPercentage: 0.6
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { display: false },
          title: {
            display: false
          }
        },
        scales: {
          x: {
            ticks: { color: this.getTextColor(), font: { size: 11 } },
            grid: { display: false }
          },
          y: {
            beginAtZero: true,
            ticks: {
              color: this.getTextColor(),
              stepSize: 1,
              font: { size: 11 }
            },
            grid: { color: 'rgba(128,128,128,0.15)' }
          }
        }
      }
    });
  }

  private destroyCharts(): void {
    this.statusChart?.destroy();
    this.priorityChart?.destroy();
    this.statusChart = null;
    this.priorityChart = null;
    this.chartsInitialized = false;
  }

  private getStatusColor(status: string): string {
    const map: Record<string, string> = {
      'PENDING': '#3b82f6',
      'IN_PROGRESS': '#f59e0b',
      'COMPLETED': '#22c55e'
    };
    return map[status] || '#6b7280';
  }

  private getPriorityColor(priority: string): string {
    const map: Record<string, string> = {
      'LOW': '#10b981',
      'MEDIUM': '#f59e0b',
      'HIGH': '#ef4444'
    };
    return map[priority] || '#6b7280';
  }

  private formatLabel(key: string): string {
    return key.replace(/_/g, ' ').replace(/\b\w/g, c => c.toUpperCase()).toLowerCase()
      .replace(/\b\w/g, c => c.toUpperCase());
  }

  private getTextColor(): string {
    const theme = document.documentElement.getAttribute('data-theme');
    return theme === 'dark' ? '#e2e8f0' : '#334155';
  }

  get completionRate(): number {
    return this.summary?.completionRate ?? 0;
  }

  getStatusPercent(status: string): number {
    if (!this.summary || !this.summary.statusCounts) return 0;
    const counts = this.summary.statusCounts;
    const total = Object.values(counts).reduce((a, b) => a + (b as number), 0);
    if (total === 0) return 0;
    return ((counts[status] || 0) / total) * 100;
  }
}
