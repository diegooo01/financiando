import { Component, OnInit, ElementRef, ViewChild, signal, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Chart, ChartConfiguration, registerables } from 'chart.js';
import { ApiService } from '../api.service';
import { CategorySpending } from '../models/models';

Chart.register(...registerables);

@Component({
  selector: 'app-spending-chart',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './spending-chart.html',
  styleUrl: './spending-chart.css'
})
export class SpendingChart implements AfterViewInit {

  @ViewChild('canvas') canvasRef!: ElementRef<HTMLCanvasElement>;

  loading = signal<boolean>(true);
  error = signal<boolean>(false);
  hasData = signal<boolean>(false);

  private chart: Chart | null = null;

  constructor(private api: ApiService) {}

  ngAfterViewInit(): void {
    this.api.getSpendingByCategory().subscribe({
      next: (data) => {
        this.loading.set(false);
        if (data.length === 0) {
          this.hasData.set(false);
          return;
        }
        this.hasData.set(true);
        this.renderChart(data);
      },
      error: () => {
        this.error.set(true);
        this.loading.set(false);
      }
    });
  }

  private renderChart(data: CategorySpending[]): void {
    const labels = data.map(d => `${d.emoji} ${d.categoryName}`);
    const values = data.map(d => d.total);

    const colors = [
      '#6366f1', '#ec4899', '#f59e0b', '#10b981',
      '#3b82f6', '#ef4444', '#8b5cf6', '#14b8a6', '#f97316'
    ];

    const config: ChartConfiguration = {
      type: 'doughnut',
      data: {
        labels: labels,
        datasets: [{
          data: values,
          backgroundColor: colors,
          borderWidth: 2,
          borderColor: '#fff'
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            position: 'right',
            labels: { padding: 12, font: { size: 13 } }
          }
        }
      }
    };

    this.chart = new Chart(this.canvasRef.nativeElement, config);
  }
}
