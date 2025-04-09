import { Component } from '@angular/core';
import { Store } from '@ngrx/store';
import { animate, style, transition, trigger } from '@angular/animations';
import {DashboardService} from "./services/dashboard/DashboardService";
import { FlatpickrOptions } from 'ng2-flatpickr';
import { FormBuilder, FormGroup } from '@angular/forms';
import * as moment from 'moment';
import {Router} from "@angular/router";

interface Product {
    serProductId: number;
    blIsDeleted: boolean;
    txtProductCode: string;
    txtProductName: string;
}

interface Deal {
    serDealId: number;
    txtDealNo: string;
}

interface TopTransaction {
    serSaleOrderId: number;
    blIsDeleted: boolean;
    blIsSales: boolean;
    dteCreateddate: number;
    dteDate: number;
    numAmount: number;
    cfgTblProduct: Product;
    slsTblDeal: Deal;
}

interface SalesData {
    transactionDifference: number;
    top6Transaction: TopTransaction[];
    onHoldCount: number;
    salesStatusDistribution: (number | null)[][];
    pendingCount: number;
    averageOrderValue: number;
    cancelledCount: number;
    completedCount: number;
    approvedCount: number;
    totalSalesAmount: number;
    salesCountByProduct: (number | null)[][];
}



class DashboardResponse {
    // @ts-ignore
    approvedCount: number;
    // @ts-ignore
    cancelledCount: number;
    // @ts-ignore
    onHoldCount: number;
    // @ts-ignore
    completedCount: number;

    // @ts-ignore
    pendingCount: number ;
    // @ts-ignore
    transactionDifference: number;
    // @ts-ignore
    lastSixTransactions: any[];
    // @ts-ignore
    percentageDifferenceApproved: number;
    // @ts-ignore
    percentageDifferenceCancelled: number;
    // @ts-ignore
    percentageDifferenceOnHold: number;
    // @ts-ignore
    percentageDifferenceCompleted: number;

    // @ts-ignore
    percentageDifferencePending: number;

    // @ts-ignore
    previousApprovedCount: number;
    // @ts-ignore
    previousCancelledCount: number;
    // @ts-ignore
    previousOnHoldCount: number;
    // @ts-ignore
    previousCompletedCount: number;
    // @ts-ignore
    previousPendingCount: number;
    salesStatusDistribution: any;
    averageOrderValue: number | undefined;
    totalSalesAmount: number | undefined;
}

@Component({
    moduleId: module.id,
    templateUrl: './finance.html',
    animations: [
        trigger('toggleAnimation', [
            transition(':enter', [style({ opacity: 0, transform: 'scale(0.95)' }), animate('100ms ease-out', style({ opacity: 1, transform: 'scale(1)' }))]),
            transition(':leave', [animate('75ms', style({ opacity: 0, transform: 'scale(0.95)' }))]),
        ]),
    ],
})
export class FinanceComponent {
    store: any;
    bitcoin: any;
    ethereum: any;
    litecoin: any;
    binance: any;
    tether: any;
    solana: any;
    isLoading = true;
    dashboardData: DashboardResponse | null = null; // To hold API response
    rangeCalendar: FlatpickrOptions;
    form3!: FormGroup;
    startDate: any = moment();
    endDate: any = moment().add(1, 'd');
    lastTransactions: any;

    salesStatusDistribution: number[] = [];
    salesStatusLabels: string[] = [];
    donutChart: any;
    // @ts-ignore
    lineChartData: { data: number[]; label: string; fill: boolean }[];
    lineChartLabels: string[] = ['Total Sales Amount', 'Average Order Value'];
    salesByCategory: any
    lineChart: any;
    pieChart: any;

    salesCountByProduct: number[] = [];
    salesProductLabels: string[] = [];
    isHead: boolean = false;

    constructor(public storeData: Store<any>, private dashboardService: DashboardService, public fb: FormBuilder, private router: Router) {
        const str = `${this.startDate.format('yyyy-MM-DD')} to ${this.endDate.format('yyyy-MM-DD')}`;
        this.form3 = this.fb.group({
            date3: [str],
        });

        this.rangeCalendar = {
            defaultDate: str,
            dateFormat: 'Y-m-d',
            mode: 'range',
            position: 'auto left',
        };
        this.initStore();
        this.isLoading = false;

    }


    drawPieChart(labels: string[], series: number[]) {

        this.pieChart = {
            series: series,
            chart: {
                height: 300,
                type: 'pie',
                zoom: {
                    enabled: false,
                },
                toolbar: {
                    show: false,
                },
            },
            labels: labels,
            colors: ['#4361ee', '#805dca', '#00ab55', '#e7515a', '#e2a03f'],
            responsive: [
                {
                    breakpoint: 480,
                    options: {
                        chart: {
                            width: 200,
                        },
                    },
                },
            ],
            stroke: {
                show: false,
            },
            legend: {
                position: 'bottom',
            },
        };
    }

    // @ts-ignore
    drawSalesByCategoryChart(labels, series) {
        const isDark = this.store.theme === 'dark' || this.store.isDarkMode;
        const isRtl = this.store.rtlClass === 'rtl';

        // @ts-ignore
        this.salesByCategory = {
            chart: {
                type: 'donut',
                height: 460,
                fontFamily: 'Nunito, sans-serif',
            },
            dataLabels: {
                enabled: false,
            },
            stroke: {
                show: true,
                width: 25,
                colors: isDark ? '#0e1726' : '#fff',
            },
            colors: isDark ? ['#5c1ac3', '#e2a03f', '#e7515a', '#e2a03f'] : ['#e2a03f', '#5c1ac3', '#e7515a'],
            legend: {
                position: 'bottom',
                horizontalAlign: 'center',
                fontSize: '14px',
                markers: {
                    width: 10,
                    height: 10,
                    offsetX: -2,
                },
                height: 50,
                offsetY: 20,
            },
            plotOptions: {
                pie: {
                    donut: {
                        size: '65%',
                        background: 'transparent',
                        labels: {
                            show: true,
                            name: {
                                show: true,
                                fontSize: '29px',
                                offsetY: -10,
                            },
                            value: {
                                show: true,
                                fontSize: '26px',
                                color: isDark ? '#bfc9d4' : undefined,
                                offsetY: 16,
                                formatter: (val: any) => val,
                            },
                            total: {
                                show: true,
                                label: 'Total',
                                color: '#888ea8',
                                fontSize: '29px',
                                formatter: (w: { globals: { seriesTotals: any[]; }; }) => {
                                    return w.globals.seriesTotals.reduce((a, b) => a + b, 0);
                                },
                            },
                        },
                    },
                },
            },
            labels: labels || ['Default Label 1', 'Default Label 2'],
            states: {
                hover: {
                    filter: {
                        type: 'none',
                        value: 0.15,
                    },
                },
                active: {
                    filter: {
                        type: 'none',
                        value: 0.15,
                    },
                },
            },
            series: series || [0, 0],
        };
    }

    ngOnInit() {
        const startDate = this.startDate.startOf('d').toISOString();
        const endDate = this.endDate.endOf('d').toISOString();
        const userJson = localStorage.getItem('user');

        // @ts-ignore
        let user: {
            cfgTblRole: number | undefined;
            serUserId: number;
        };
        if (userJson) {
            // @ts-ignore
            user = JSON.parse(userJson) as CfgTblUser;
        }

        // @ts-ignore
        // Check if the user is Marketing Head
        // this.loadPermissionRoles(user.cfgTblRole.serRoleId,user.serUserId);

        // @ts-ignore
        if (user.cfgTblRole?.txtRoleName === 'ADMIN') {
            this.isHead = true;
        } else {
            this.isHead = false;
        }

        this.fetchDashboardData(startDate, endDate);
    }

    onDateChange($event: any) {
        console.log($event.target.value);
        const dateRange = $event.target.value.split('to').map((v: any) => v.trim());
        const start = moment(dateRange[0]).startOf('d').toISOString();
        const end = moment(dateRange[1]).endOf('d').toISOString();
        if (dateRange && dateRange.length === 2) {
            this.fetchDashboardData(start, end);
        }
    }

    async initStore() {
        this.storeData
            .select((d) => d.index)
            .subscribe((d) => {
                const hasChangeTheme = this.store?.theme !== d?.theme;
                const hasChangeLayout = this.store?.layout !== d?.layout;
                const hasChangeMenu = this.store?.menu !== d?.menu;
                const hasChangeSidebar = this.store?.sidebar !== d?.sidebar;

                this.store = d;

                if (hasChangeTheme || hasChangeLayout || hasChangeMenu || hasChangeSidebar) {
                    if (this.isLoading || hasChangeTheme) {
                        this.initCharts(); //init charts
                    } else {
                        setTimeout(() => {
                            this.initCharts(); // refresh charts
                        }, 300);
                    }
                }
            });
    }

    fetchDashboardData(startDate: string, endDate: string) {
        this.isLoading = true;
        // this.lastTransactions = undefined;
        this.dashboardService.getDashboardData(startDate, endDate).subscribe(
            (data) => {
                // @ts-ignore
                this.dashboardData = data;

                // @ts-ignore
                this.dashboardData.lastSixTransactions = this.dashboardData.top6Transaction;
                // @ts-ignore
                this.dashboardData.totalSalesAmount = data.totalSalesAmount;
                // @ts-ignore
                this.dashboardData.averageOrderValue = data.averageOrderValue;
                if (this.dashboardData && this.dashboardData.lastSixTransactions && this.dashboardData.lastSixTransactions.length) {
                    this.lastTransactions = this.dashboardData.lastSixTransactions;
                    this.lastTransactions.forEach((txn: any) => {
                        txn.dteDate = moment(txn.dteDate).format('MM/DD/YYYY');
                    });
                }
                // @ts-ignore
                this.salesStatusLabels = this.dashboardData?.salesStatusDistribution.map(item => {
                    return Array.isArray(item) ? item[0] : item; // Adjust according to actual structure
                });

                // @ts-ignore
                this.salesStatusDistribution = this.dashboardData?.salesStatusDistribution.map(item => {
                    return Array.isArray(item) ? item[1] : item; // Adjust according to actual structure
                });

                console.log("labels", this.salesStatusLabels);
                console.log("values", this.salesStatusDistribution);
                this.drawSalesByCategoryChart(this.salesStatusLabels, this.salesStatusDistribution);

                // @ts-ignore
                this.salesProductLabels = this.dashboardData?.salesCountByProduct.map(item => {
                    return Array.isArray(item) ? item[0] : item; // Adjust according to actual structure
                });

                // @ts-ignore
                this.salesCountByProduct = this.dashboardData?.salesCountByProduct.map(item => {
                    return Array.isArray(item) ? item[1] : item;
                });
                console.log("labels", this.salesProductLabels);
                console.log("values", this.salesCountByProduct);

                this.drawPieChart(this.salesProductLabels, this.salesCountByProduct)
                this.isLoading = false;
                this.initCharts();
            },
            (error) => {
                console.error('Error fetching dashboard data', error);
                this.isLoading = false;
            }
        );
    }


    prepareChartData(averageOrderValue: any, totalSalesAmount: any): void {
        this.lineChartData = [
            {
                data: [totalSalesAmount, averageOrderValue],
                label: 'Sales Overview',
                fill: true,
            }
        ];
    }


    prepareSalesStatusDistribution(distribution: any): void {
        this.salesStatusLabels = Object.keys(distribution);
        this.salesStatusDistribution = Object.values(distribution);
    }

    initCharts() {
        // bitcoin
        this.bitcoin = {
            chart: {
                height: 45,
                type: 'line',
                sparkline: {
                    enabled: true,
                },
            },
            stroke: {
                width: 2,
            },
            markers: {
                size: 0,
            },
            colors: ['#00ab55'],
            grid: {
                padding: {
                    top: 0,
                    bottom: 0,
                    left: 0,
                },
            },
            tooltip: {
                x: {
                    show: false,
                },
                y: {
                    title: {
                        formatter: (val: any) => {
                            return '';
                        },
                    },
                },
            },
            responsive: [
                {
                    breakPoint: 576,
                    options: {
                        chart: {
                            height: 95,
                        },
                        grid: {
                            padding: {
                                top: 45,
                                bottom: 0,
                                left: 0,
                            },
                        },
                    },
                },
            ],
            series: [
                {
                    data: [21, 9, 36, 12, 44, 25, 59, 41, 25, 66],
                },
            ],
        };

        // ethereum
        this.ethereum = {
            chart: {
                height: 45,
                type: 'line',
                sparkline: {
                    enabled: true,
                },
            },
            stroke: {
                width: 2,
            },
            markers: {
                size: 0,
            },
            colors: ['#e7515a'],
            grid: {
                padding: {
                    top: 0,
                    bottom: 0,
                    left: 0,
                },
            },
            tooltip: {
                x: {
                    show: false,
                },
                y: {
                    title: {
                        formatter: (val: any) => {
                            return '';
                        },
                    },
                },
            },
            responsive: [
                {
                    breakPoint: 576,
                    options: {
                        chart: {
                            height: 95,
                        },
                        grid: {
                            padding: {
                                top: 45,
                                bottom: 0,
                                left: 0,
                            },
                        },
                    },
                },
            ],
            series: [
                {
                    data: [44, 25, 59, 41, 66, 25, 21, 9, 36, 12],
                },
            ],
        };

        // litecoin
        this.litecoin = {
            chart: {
                height: 45,
                type: 'line',
                sparkline: {
                    enabled: true,
                },
            },
            stroke: {
                width: 2,
            },
            markers: {
                size: 0,
            },
            colors: ['#00ab55'],
            grid: {
                padding: {
                    top: 0,
                    bottom: 0,
                    left: 0,
                },
            },
            tooltip: {
                x: {
                    show: false,
                },
                y: {
                    title: {
                        formatter: (val: any) => {
                            return '';
                        },
                    },
                },
            },
            responsive: [
                {
                    breakPoint: 576,
                    options: {
                        chart: {
                            height: 95,
                        },
                        grid: {
                            padding: {
                                top: 45,
                                bottom: 0,
                                left: 0,
                            },
                        },
                    },
                },
            ],
            series: [
                {
                    data: [9, 21, 36, 12, 66, 25, 44, 25, 41, 59],
                },
            ],
        };

        // binance
        this.binance = {
            chart: {
                height: 45,
                type: 'line',
                sparkline: {
                    enabled: true,
                },
            },
            stroke: {
                width: 2,
            },
            markers: {
                size: 0,
            },
            colors: ['#e7515a'],
            grid: {
                padding: {
                    top: 0,
                    bottom: 0,
                    left: 0,
                },
            },
            tooltip: {
                x: {
                    show: false,
                },
                y: {
                    title: {
                        formatter: (val: any) => {
                            return '';
                        },
                    },
                },
            },
            responsive: [
                {
                    breakPoint: 576,
                    options: {
                        chart: {
                            height: 95,
                        },
                        grid: {
                            padding: {
                                top: 45,
                                bottom: 0,
                                left: 0,
                            },
                        },
                    },
                },
            ],
            series: [
                {
                    data: [25, 44, 25, 59, 41, 21, 36, 12, 19, 9],
                },
            ],
        };

        // tether
        this.tether = {
            chart: {
                height: 45,
                type: 'line',
                sparkline: {
                    enabled: true,
                },
            },
            stroke: {
                width: 2,
            },
            markers: {
                size: 0,
            },
            colors: ['#00ab55'],
            grid: {
                padding: {
                    top: 0,
                    bottom: 0,
                    left: 0,
                },
            },
            tooltip: {
                x: {
                    show: false,
                },
                y: {
                    title: {
                        formatter: (val: any) => {
                            return '';
                        },
                    },
                },
            },
            responsive: [
                {
                    breakPoint: 576,
                    options: {
                        chart: {
                            height: 95,
                        },
                        grid: {
                            padding: {
                                top: 45,
                                bottom: 0,
                                left: 0,
                            },
                        },
                    },
                },
            ],
            series: [
                {
                    data: [21, 59, 41, 44, 25, 66, 9, 36, 25, 12],
                },
            ],
        };

        // solana
        this.solana = {
            chart: {
                height: 45,
                type: 'line',
                sparkline: {
                    enabled: true,
                },
            },
            stroke: {
                width: 2,
            },
            markers: {
                size: 0,
            },
            colors: ['#e7515a'],
            grid: {
                padding: {
                    top: 0,
                    bottom: 0,
                    left: 0,
                },
            },
            tooltip: {
                x: {
                    show: false,
                },
                y: {
                    title: {
                        formatter: (val: any) => {
                            return '';
                        },
                    },
                },
            },
            responsive: [
                {
                    breakPoint: 576,
                    options: {
                        chart: {
                            height: 95,
                        },
                        grid: {
                            padding: {
                                top: 45,
                                bottom: 0,
                                left: 0,
                            },
                        },
                    },
                },
            ],
            series: [
                {
                    data: [21, -9, 36, -12, 44, 25, 59, -41, 66, -25],
                },
            ],
        };
    }


    navigateToPendingTransactions(): void {
        const formValue = this.form3.value;
// Access the date3 field
        const dateRange = formValue.date3;

        let startDate: string | undefined;
        let endDate: string | undefined;

    // Utility function to format date into "YYYY-MM-DD"
        const formatDate = (date: Date | string): string => {
            if (date instanceof Date) {
                return date.toISOString().split('T')[0]; // Extract "YYYY-MM-DD"
            } else if (typeof date === 'string') {
                const parsedDate = new Date(date);
                if (!isNaN(parsedDate.getTime())) {
                    return parsedDate.toISOString().split('T')[0];
                }
            }
            return '';
        };

        // Handle both scenarios: array or "YYYY-MM-DD to YYYY-MM-DD" string
        if (dateRange) {
            if (Array.isArray(dateRange) && dateRange.length === 2) {
                // Case 1: dateRange is an array of Date objects or strings
                startDate = formatDate(dateRange[0]);
                endDate = formatDate(dateRange[1]);
            } else if (typeof dateRange === 'string' && dateRange.includes('to')) {
                // Case 2: dateRange is a string "YYYY-MM-DD to YYYY-MM-DD"
                const dates = dateRange.split('to').map(date => formatDate(date.trim()));
                if (dates.length === 2) {
                    startDate = moment(dates[0]).startOf('d').toISOString();
                   endDate= moment(dates[1]).endOf('d').toISOString();
                } else {
                    console.error('Date range string format is invalid.');
                }
            } else {
                console.error('Date range format is invalid.');
            }
        } else {
            console.error('Date range is not selected.');
        }

    // Define status flag
        const statusFlag = 'PENDING';
        // @ts-ignore
        startDate = new Date(startDate).toISOString();
        // @ts-ignore
        endDate = new Date(endDate).toISOString();
        if (startDate && endDate) {
            console.log('Start Date:', new Date(startDate).toISOString());
            console.log('End Date:', new Date(endDate).toISOString());
        } else {
            console.error('Failed to extract start and end dates.');
        }
    // Navigate to the transactions-details route with query parameters
        this.router.navigate(['/transactions-details'], {
            queryParams: { startDate, endDate, status: statusFlag }
        });

    }

    navigateToHoldTransactions(): void {
        /* const startDate = this.startDate.startOf('d').toISOString();
        const endDate = this.endDate.endOf('d').toISOString();*/
        const formValue = this.form3.value;
// Access the date3 field
        const dateRange = formValue.date3;

        let startDate: string | undefined;
        let endDate: string | undefined;

        // Utility function to format date into "YYYY-MM-DD"
        const formatDate = (date: Date | string): string => {
            if (date instanceof Date) {
                return date.toISOString().split('T')[0]; // Extract "YYYY-MM-DD"
            } else if (typeof date === 'string') {
                const parsedDate = new Date(date);
                if (!isNaN(parsedDate.getTime())) {
                    return parsedDate.toISOString().split('T')[0];
                }
            }
            return '';
        };

        // Handle both scenarios: array or "YYYY-MM-DD to YYYY-MM-DD" string
        if (dateRange) {
            if (Array.isArray(dateRange) && dateRange.length === 2) {
                // Case 1: dateRange is an array of Date objects or strings
                startDate = formatDate(dateRange[0]);
                endDate = formatDate(dateRange[1]);
            } else if (typeof dateRange === 'string' && dateRange.includes('to')) {
                // Case 2: dateRange is a string "YYYY-MM-DD to YYYY-MM-DD"
                const dates = dateRange.split('to').map(date => formatDate(date.trim()));
                if (dates.length === 2) {
                    startDate = moment(dates[0]).startOf('d').toISOString();
                    endDate= moment(dates[1]).endOf('d').toISOString();
                } else {
                    console.error('Date range string format is invalid.');
                }
            } else {
                console.error('Date range format is invalid.');
            }
        } else {
            console.error('Date range is not selected.');
        }

        // Define status flag
        const statusFlag = 'Hold';
        // @ts-ignore
        startDate = new Date(startDate).toISOString();
        // @ts-ignore
        endDate = new Date(endDate).toISOString();
        if (startDate && endDate) {
            console.log('Start Date:', new Date(startDate).toISOString());
            console.log('End Date:', new Date(endDate).toISOString());
        } else {
            console.error('Failed to extract start and end dates.');
        }
        // Navigate to the transactions-details route with query parameters
        this.router.navigate(['/transactions-details'], {
            queryParams: { startDate, endDate, status: statusFlag }
        });
    }


    navigateToCancelTransactions(): void {
        /* const startDate = this.startDate.startOf('d').toISOString();
        const endDate = this.endDate.endOf('d').toISOString();*/
        const formValue = this.form3.value;
// Access the date3 field
        const dateRange = formValue.date3;

        let startDate: string | undefined;
        let endDate: string | undefined;

        // Utility function to format date into "YYYY-MM-DD"
        const formatDate = (date: Date | string): string => {
            if (date instanceof Date) {
                return date.toISOString().split('T')[0]; // Extract "YYYY-MM-DD"
            } else if (typeof date === 'string') {
                const parsedDate = new Date(date);
                if (!isNaN(parsedDate.getTime())) {
                    return parsedDate.toISOString().split('T')[0];
                }
            }
            return '';
        };

        // Handle both scenarios: array or "YYYY-MM-DD to YYYY-MM-DD" string
        if (dateRange) {
            if (Array.isArray(dateRange) && dateRange.length === 2) {
                // Case 1: dateRange is an array of Date objects or strings
                startDate = formatDate(dateRange[0]);
                endDate = formatDate(dateRange[1]);
            } else if (typeof dateRange === 'string' && dateRange.includes('to')) {
                // Case 2: dateRange is a string "YYYY-MM-DD to YYYY-MM-DD"
                const dates = dateRange.split('to').map(date => formatDate(date.trim()));
                if (dates.length === 2) {
                    startDate = moment(dates[0]).startOf('d').toISOString();
                    endDate= moment(dates[1]).endOf('d').toISOString();
                } else {
                    console.error('Date range string format is invalid.');
                }
            } else {
                console.error('Date range format is invalid.');
            }
        } else {
            console.error('Date range is not selected.');
        }

        // Define status flag
        const statusFlag = 'cancel';
        // @ts-ignore
        startDate = new Date(startDate).toISOString();
        // @ts-ignore
        endDate = new Date(endDate).toISOString();
        if (startDate && endDate) {
            console.log('Start Date:', new Date(startDate).toISOString());
            console.log('End Date:', new Date(endDate).toISOString());
        } else {
            console.error('Failed to extract start and end dates.');
        }
        // Navigate to the transactions-details route with query parameters
        this.router.navigate(['/transactions-details'], {
            queryParams: { startDate, endDate, status: statusFlag }
        });

    }

    navigateToApprovedTransactions(): void {
        /* const startDate = this.startDate.startOf('d').toISOString();
        const endDate = this.endDate.endOf('d').toISOString();*/
        const formValue = this.form3.value;
// Access the date3 field
        const dateRange = formValue.date3;

        let startDate: string | undefined;
        let endDate: string | undefined;

        // Utility function to format date into "YYYY-MM-DD"
        const formatDate = (date: Date | string): string => {
            if (date instanceof Date) {
                return date.toISOString().split('T')[0]; // Extract "YYYY-MM-DD"
            } else if (typeof date === 'string') {
                const parsedDate = new Date(date);
                if (!isNaN(parsedDate.getTime())) {
                    return parsedDate.toISOString().split('T')[0];
                }
            }
            return '';
        };

        // Handle both scenarios: array or "YYYY-MM-DD to YYYY-MM-DD" string
        if (dateRange) {
            if (Array.isArray(dateRange) && dateRange.length === 2) {
                // Case 1: dateRange is an array of Date objects or strings
                startDate = formatDate(dateRange[0]);
                endDate = formatDate(dateRange[1]);
            } else if (typeof dateRange === 'string' && dateRange.includes('to')) {
                // Case 2: dateRange is a string "YYYY-MM-DD to YYYY-MM-DD"
                const dates = dateRange.split('to').map(date => formatDate(date.trim()));
                if (dates.length === 2) {
                    startDate = moment(dates[0]).startOf('d').toISOString();
                    endDate= moment(dates[1]).endOf('d').toISOString();
                } else {
                    console.error('Date range string format is invalid.');
                }
            } else {
                console.error('Date range format is invalid.');
            }
        } else {
            console.error('Date range is not selected.');
        }

        // Define status flag
        const statusFlag = 'Approved';
        // @ts-ignore
        startDate = new Date(startDate).toISOString();
        // @ts-ignore
        endDate = new Date(endDate).toISOString();
        if (startDate && endDate) {
            console.log('Start Date:', new Date(startDate).toISOString());
            console.log('End Date:', new Date(endDate).toISOString());
        } else {
            console.error('Failed to extract start and end dates.');
        }
        // Navigate to the transactions-details route with query parameters
        this.router.navigate(['/transactions-details'], {
            queryParams: { startDate, endDate, status: statusFlag }
        });

    }
}
