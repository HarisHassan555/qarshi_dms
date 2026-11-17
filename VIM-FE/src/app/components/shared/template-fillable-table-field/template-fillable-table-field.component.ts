import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import {
    applyFillableTableStructureChange,
    applyTableCellValueChange,
    buildTableRenderRows,
    getFormulaDefinitionForCellKey,
    getTableIndicatorColumnCount,
    normalizeTableCellValues,
    normalizeTableHtmlForField,
    TableFieldLike,
    TableRenderCell,
    TableRenderRow,
} from 'src/app/utils/template-fillable-table-field.util';
import {
    getFormulaEditText,
    isExcelFormulaComplete,
    isExcelFormulaText,
    shiftExcelFormulaReferences,
} from 'src/app/utils/template-table-formula.util';

let lastCopiedTableFormula: { expression: string; row: number; col: number } | null = null;

@Component({
    selector: 'app-template-fillable-table-field',
    templateUrl: './template-fillable-table-field.component.html',
    styleUrls: ['./template-fillable-table-field.component.css'],
})
export class TemplateFillableTableFieldComponent implements OnChanges {
    @Input() field!: TableFieldLike;
    @Input()
    set cellValues(value: Record<string, string>) {
        this.internalCellValues = normalizeTableCellValues(value);
    }
    get cellValues(): Record<string, string> {
        return this.internalCellValues;
    }
    @Input() editable = false;
    @Input() inline = false;
    @Output() cellValuesChange = new EventEmitter<Record<string, string>>();
    @Output() tableHtmlChange = new EventEmitter<string>();

    tableFieldIndicatorsDismissed = false;
    structureMenu: { key: string; top: number; left: number } | null = null;
    private tableRenderRowsCache: { html: string; rows: TableRenderRow[] } | null = null;
    private editingTableFormulaKey: string | null = null;
    private tableFormulaDrafts = new Map<string, string>();
    private internalCellValues: Record<string, string> = {};

    ngOnChanges(changes: SimpleChanges): void {
        if (changes['cellValues'] || changes['field']) {
            this.tableRenderRowsCache = null;
        }
    }

    private applyTableMutation(result: { values: Record<string, string>; tableHtml: string }): void {
        this.field.tableHtml = result.tableHtml;
        this.internalCellValues = { ...result.values };
        this.tableRenderRowsCache = null;
    }

    dismissTableFieldIndicators(event: Event): void {
        event.preventDefault();
        event.stopPropagation();
        this.tableFieldIndicatorsDismissed = true;
    }

    getTableIndicatorColumns(): string[] {
        return getTableIndicatorColumnCount(this.field);
    }

    get tableRowCount(): number {
        return this.getTableRenderRows().length;
    }

    get tableColumnCount(): number {
        return this.getTableIndicatorColumns().length;
    }

    addTableRow(placement: 'above' | 'below', index: number, event: Event): void {
        this.commitTableStructure(placement === 'above' ? 'add-row-above' : 'add-row-below', event, index);
    }

    deleteTableRow(index: number, event: Event): void {
        this.commitTableStructure('delete-row', event, index);
    }

    addTableColumn(placement: 'before' | 'after', index: number, event: Event): void {
        this.commitTableStructure(placement === 'before' ? 'add-column-before' : 'add-column-after', event, index);
    }

    deleteTableColumn(index: number, event: Event): void {
        this.commitTableStructure('delete-column', event, index);
    }

    getTableRenderRows(): TableRenderRow[] {
        const html = normalizeTableHtmlForField(this.field, this.field.tableHtml);
        if (this.tableRenderRowsCache && this.tableRenderRowsCache.html === html) {
            return this.tableRenderRowsCache.rows;
        }
        const rows = buildTableRenderRows(this.field, html);
        this.tableRenderRowsCache = { html, rows };
        return rows;
    }

    getTableCellValue(cellKey: string): string {
        return normalizeTableCellValues(this.cellValues)[cellKey] || '';
    }

    tableFormulaInputValue(cellKey: string): string {
        if (this.isTableFormulaEditing(cellKey)) {
            return this.getTableFormulaExpression(cellKey);
        }
        return this.getTableCellValue(cellKey);
    }

    tableCellEditorValue(cell: TableRenderCell): string {
        const draft = this.tableFormulaDrafts.get(this.tableFormulaDraftKey(cell.key));
        if (this.isTableFormulaEditing(cell.key) || (draft !== undefined && isExcelFormulaText(draft) && !isExcelFormulaComplete(draft))) {
            return draft !== undefined ? draft : this.getTableFormulaExpression(cell.key);
        }
        if (cell.formula && !this.isTableFormulaEditing(cell.key)) {
            const shown = this.getTableCellValue(cell.key);
            if (shown === '#ERR') {
                const expression = this.getTableFormulaExpression(cell.key);
                if (expression && !isExcelFormulaComplete(expression)) {
                    return expression;
                }
            }
        }
        return cell.formula ? this.tableFormulaInputValue(cell.key) : this.getTableCellValue(cell.key);
    }

    isTableFormulaEditing(cellKey: string): boolean {
        return this.editingTableFormulaKey === this.tableFormulaDraftKey(cellKey);
    }

    isStructureMenuOpen(cellKey: string): boolean {
        return this.editable && this.structureMenu?.key === cellKey;
    }

    openStructureMenu(cell: TableRenderCell, event: Event): void {
        if (!this.editable) {
            return;
        }
        const target = event.currentTarget as HTMLElement | null;
        const anchor = target?.closest('td') || target;
        const rect = anchor?.getBoundingClientRect();
        this.structureMenu = {
            key: cell.key,
            top: (rect?.bottom || 0) + 4,
            left: rect?.left || 0,
        };
        if (cell.formula) {
            this.beginTableFormulaEdit(cell.key);
        }
    }

    beginTableCellEdit(cell: TableRenderCell, event: FocusEvent): void {
        this.openStructureMenu(cell, event);
        event.stopPropagation();
    }

    endTableCellEdit(cell: TableRenderCell): void {
        this.endTableFormulaEdit(cell.key);
        setTimeout(() => {
            if (this.structureMenu?.key === cell.key && document.activeElement?.closest('.fill-table-cell-menu') == null) {
                this.structureMenu = null;
            }
        });
    }

    onTableCellCopy(cell: TableRenderCell, event: ClipboardEvent): void {
        const expression = this.getTableFormulaExpression(cell.key);
        if (!isExcelFormulaText(expression)) {
            return;
        }
        const input = event.target as HTMLInputElement | null;
        const start = input?.selectionStart ?? 0;
        const end = input?.selectionEnd ?? 0;
        const value = input?.value || '';
        const entireValueSelected = !!value && start === 0 && end === value.length;
        const copyingCalculatedResult = !isExcelFormulaText(value);
        if (value && !entireValueSelected && !copyingCalculatedResult) {
            return;
        }
        event.preventDefault();
        event.clipboardData?.setData('text/plain', expression);
        const position = this.excelPosition(cell.key);
        lastCopiedTableFormula = position
            ? { expression: this.normalizeCopiedFormula(expression), row: position.row, col: position.col }
            : null;
    }

    onTableCellPaste(cell: TableRenderCell, event: ClipboardEvent): void {
        if (!this.editable) {
            return;
        }
        const pasted = String(event.clipboardData?.getData('text/plain') || '').trim();
        if (!isExcelFormulaText(pasted)) {
            return;
        }
        event.preventDefault();
        const position = this.excelPosition(cell.key);
        const source = lastCopiedTableFormula;
        const expression = position && source && this.normalizeCopiedFormula(pasted) === source.expression
            ? shiftExcelFormulaReferences(pasted, position.row - source.row, position.col - source.col)
            : pasted;
        this.structureMenu = null;
        if (!this.isTableFormulaEditing(cell.key)) {
            this.beginTableFormulaEdit(cell.key);
        }
        this.setTableFormulaExpression(cell.key, expression);
    }

    onTableCellInput(cell: TableRenderCell, value: string): void {
        this.structureMenu = null;
        if (cell.formula || this.isTableFormulaEditing(cell.key) || isExcelFormulaText(value)) {
            if (!this.isTableFormulaEditing(cell.key)) {
                this.beginTableFormulaEdit(cell.key);
            }
            this.setTableFormulaExpression(cell.key, value);
            return;
        }
        const result = applyTableCellValueChange(this.field, this.cellValues, cell.key, value);
        this.applyTableMutation(result);
        this.cellValuesChange.emit(result.values);
        this.tableHtmlChange.emit(result.tableHtml);
    }

    trackTableRow(_index: number, row: TableRenderRow): string {
        return row.cells.map((cell) => cell.key).join('|');
    }

    trackTableCell(_index: number, cell: TableRenderCell): string {
        return cell.key;
    }

    private commitTableStructure(
        action: 'add-row-above' | 'add-row-below' | 'add-column-before' | 'add-column-after' | 'delete-row' | 'delete-column',
        event: Event,
        index?: number
    ): void {
        event.preventDefault();
        event.stopPropagation();
        if (!this.editable) {
            return;
        }
        const result = applyFillableTableStructureChange(this.field, this.cellValues, action, index);
        if (!result) {
            return;
        }
        this.applyTableMutation(result);
        this.tableFormulaDrafts.clear();
        this.editingTableFormulaKey = null;
        this.structureMenu = null;
        this.cellValuesChange.emit(result.values);
        this.tableHtmlChange.emit(result.tableHtml);
    }

    private excelPosition(cellKey: string): { row: number; col: number } | null {
        const match = /^r(\d+)c(\d+)$/.exec(cellKey);
        if (!match) {
            return null;
        }
        return { row: Number(match[1]) + 1, col: Number(match[2]) + 1 };
    }

    private normalizeCopiedFormula(expression: string): string {
        return String(expression || '').replace(/\s+/g, '').toUpperCase();
    }

    private tableFormulaDraftKey(cellKey: string): string {
        return `${this.field.id}:${cellKey}`;
    }

    private beginTableFormulaEdit(cellKey: string): void {
        this.editingTableFormulaKey = this.tableFormulaDraftKey(cellKey);
    }

    private endTableFormulaEdit(cellKey: string): void {
        const draftKey = this.tableFormulaDraftKey(cellKey);
        if (this.editingTableFormulaKey !== draftKey) {
            return;
        }
        const draft = this.tableFormulaDrafts.get(draftKey);
        this.editingTableFormulaKey = null;
        if (draft !== undefined && isExcelFormulaComplete(draft)) {
            this.tableFormulaDrafts.delete(draftKey);
        }
    }

    private getTableFormulaExpression(cellKey: string): string {
        const draft = this.tableFormulaDrafts.get(this.tableFormulaDraftKey(cellKey));
        if (draft !== undefined) {
            return draft;
        }
        const definition = getFormulaDefinitionForCellKey(this.field, cellKey);
        return definition ? getFormulaEditText(definition) : '';
    }

    private setTableFormulaExpression(cellKey: string, expression: string): void {
        const trimmed = String(expression || '').trim();
        const draftKey = this.tableFormulaDraftKey(cellKey);
        if (!trimmed) {
            this.tableFormulaDrafts.delete(draftKey);
            const result = applyTableCellValueChange(this.field, this.cellValues, cellKey, '');
            this.applyTableMutation(result);
            this.cellValuesChange.emit(result.values);
            this.tableHtmlChange.emit(result.tableHtml);
            return;
        }
        if (!isExcelFormulaText(trimmed)) {
            this.tableFormulaDrafts.delete(draftKey);
            const result = applyTableCellValueChange(this.field, this.cellValues, cellKey, trimmed);
            this.applyTableMutation(result);
            this.cellValuesChange.emit(result.values);
            this.tableHtmlChange.emit(result.tableHtml);
            return;
        }
        if (!isExcelFormulaComplete(trimmed)) {
            this.tableFormulaDrafts.set(draftKey, trimmed);
            const result = applyTableCellValueChange(this.field, this.cellValues, cellKey, trimmed);
            this.applyTableMutation(result);
            this.cellValuesChange.emit(result.values);
            this.tableHtmlChange.emit(result.tableHtml);
            return;
        }
        this.tableFormulaDrafts.set(draftKey, trimmed);
        const result = applyTableCellValueChange(this.field, this.cellValues, cellKey, trimmed);
        this.applyTableMutation(result);
        this.cellValuesChange.emit(result.values);
        this.tableHtmlChange.emit(result.tableHtml);
    }
}
