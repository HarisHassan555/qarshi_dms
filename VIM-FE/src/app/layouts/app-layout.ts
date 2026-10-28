import { Component } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppService } from '../service/app.service';
import { Router, NavigationEnd } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { filter } from 'rxjs/operators';

@Component({
    selector: 'app-root',
    templateUrl: './app-layout.html',
})
export class AppLayout {
    store: any = { sidebar: false, menu: '', layout: '', rtlClass: '', isShowMainLoader: false, navbar: '' };
    showTopButton = false;
    hideAppChrome = false;
    constructor(public translate: TranslateService, public storeData: Store<any>, private service: AppService, private router: Router) {
        this.initStore();
        this.updateShareRouteFlag();
    }
    headerClass = '';

    get isDocumentShareRoute(): boolean {
        return this.hideAppChrome;
    }

    ngOnInit() {
        this.router.events
            .pipe(filter((event) => event instanceof NavigationEnd))
            .subscribe(() => this.updateShareRouteFlag());
        this.initAnimation();
        setTimeout(() => this.toggleLoader(), 0);
        window.addEventListener('scroll', () => {
            if (document.body.scrollTop > 50 || document.documentElement.scrollTop > 50) {
                this.showTopButton = true;
            } else {
                this.showTopButton = false;
            }
        });
    }

    ngOnDestroy() {
        window.removeEventListener('scroll', () => {});
    }

    initAnimation() {
        this.service.changeAnimation();
        this.router.events.subscribe((event) => {
            if (event instanceof NavigationEnd) {
                this.service.changeAnimation();
            }
        });

        const ele: any = document.querySelector('.animation');
        ele.addEventListener('animationend', () => {
            this.service.changeAnimation('remove');
        });
    }

    toggleLoader() {
        this.storeData.dispatch({ type: 'toggleMainLoader', payload: true });
        setTimeout(() => {
            this.storeData.dispatch({ type: 'toggleMainLoader', payload: false });
        }, 500);
    }

    async initStore() {
        this.storeData
            .select((d) => d.index)
            .subscribe((d) => {
                this.store = d;
            });
    }

    goToTop() {
        document.body.scrollTop = 0;
        document.documentElement.scrollTop = 0;
    }

    private updateShareRouteFlag(): void {
        let path = (this.router.url || '').split('?')[0] || '';
        path = path.replace(/^\/velocity\/?/i, '/');
        if (!path.startsWith('/')) {
            path = '/' + path;
        }
        const segments = path.split('/').filter(Boolean);
        this.hideAppChrome = segments[0] === 'share';
    }
}
