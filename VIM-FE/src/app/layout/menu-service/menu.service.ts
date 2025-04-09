import {HttpClient, HttpHeaders, HttpParams} from '@angular/common/http';
import { Injectable } from '@angular/core';
import { urls } from 'src/app/utils/urls';
import {Observable} from "rxjs";

@Injectable({
  providedIn: 'root'
})
export class MenuService {

  constructor(
    private http: HttpClient
  ) { }

  getUserMenus() {
    return this.http.get(urls.API_URL + 'allMenu');
  }


    // @ts-ignore
    getAllSubMenuRoles(roleId: number | undefined, userId: number): Observable<CfgTblSubMenuRole[]> {
        // @ts-ignore
        // @ts-ignore
        // @ts-ignore
        const params = new HttpParams()
            .set('roleId', Number(roleId))
            .set('userId', Number(userId));

        // @ts-ignore
        return this.http.get<CfgTblSubMenuRole[]>(urls.API_URL + 'allSubMenuRole', { params });
    }



    // @ts-ignore
    saveSubMenuRole(userId: string, roleId: string,subMenuRole: any): Observable<CfgTblSubMenuRole> {

      // @ts-ignore
        return this.http.post<CfgTblSubMenuRole>(`${urls.API_URL}addNewSubMenuRoleinList/${userId}/${roleId}`,subMenuRole)
    }


    getMenuBySubMenu(id: any) {
        const params = new HttpParams()
            .set('Id', Number(id))
        return this.http.get(urls.API_URL + 'getMenuBySubmenuId',{ params });
    }
}
