import { AfterViewInit, Component, ElementRef, OnDestroy, ViewChild } from '@angular/core';
import { DocSelectionManagerService, RichTextEditingMutation } from '@univerjs/docs';
import { IDocClipboardService } from '@univerjs/docs-ui';
import { UniverDocsCorePreset } from '@univerjs/preset-docs-core';
import UniverPresetDocsCoreEnUS from '@univerjs/preset-docs-core/lib/locales/en-US';
import { createUniver, DocumentFlavor, IUniverInstanceService, JSONX, LocaleType, mergeLocales, PAGE_SIZE, PageOrientType, PaperType, UniverInstanceType } from '@univerjs/presets';
import '@univerjs/ui/facade';

type BuilderMode = 'sheet' | 'document';
type Density = 'compact' | 'comfortable' | 'spacious';
type TextAlign = 'left' | 'center' | 'right';
type CellFormat = 'general' | 'currency' | 'percent' | 'date';
type RibbonTab = 'home' | 'insert' | 'data' | 'view';
type ContextMenuType = 'cell' | 'row' | 'column';
type PageOrientation = 'portrait' | 'landscape';

type UniverApi = {
    createDocument: (data: Record<string, unknown>) => unknown;
    executeCommand: <P extends object = object, R = boolean>(id: string, params?: P) => Promise<R>;
    createMenu?: (menuItem: { id: string; title: string; tooltip?: string; action: () => void; order?: number }) => {
        appendTo: (path: string | string[]) => void;
    };
};

type UniverInstance = {
    dispose: () => void;
    __getInjector: () => {
        get: <T>(key: unknown) => T;
    };
};

interface UniverTableRange {
    tableId: string;
    startRow: number;
    endRow: number;
    startColumn: number;
    endColumn: number;
    segmentId?: string;
}

interface FormCanvasStyle {
    background: string;
    border: string;
    accent: string;
    radius: number;
    density: Density;
}

interface CellStyle {
    fontFamily: string;
    fontSize: number;
    bold: boolean;
    italic: boolean;
    underline: boolean;
    textAlign: TextAlign;
    color: string;
    background: string;
    borderColor: string;
    wrap: boolean;
}

interface SheetCell extends CellStyle {
    value: string;
    format: CellFormat;
    rowSpan: number;
    colSpan: number;
    hidden: boolean;
    masterRow?: number;
    masterCol?: number;
}

@Component({
    selector: 'app-univer-form-builder',
    templateUrl: './univer-form-builder.component.html',
    styleUrls: ['./univer-form-builder.component.css']
})
export class UniverFormBuilderComponent implements AfterViewInit, OnDestroy {
    @ViewChild('wordEditor') wordEditor?: ElementRef<HTMLDivElement>;
    @ViewChild('univerDocsHost') univerDocsHost?: ElementRef<HTMLDivElement>;

    activeMode: BuilderMode = 'sheet';
    activeRibbonTab: RibbonTab = 'home';
    pageOrientation: PageOrientation = 'portrait';
    zoom = 100;
    sheetRowCount = 12;
    sheetColumnCount = 6;
    mergeRowCount = 1;
    mergeColumnCount = 2;
    freezeRowCount = 1;
    freezeColumnCount = 1;
    filterText = '';
    rowHeights: number[] = [];
    columnWidths: number[] = [];
    selectedCell = { row: 0, col: 0 };
    selectionStart = { row: 0, col: 0 };
    selectionEnd = { row: 0, col: 0 };
    isSelecting = false;
    contextMenu = {
        visible: false,
        x: 0,
        y: 0,
        type: 'cell' as ContextMenuType
    };
    sheetData: SheetCell[][] = [];
    wordFontFamily = 'Calibri';
    wordFontSize = 14;
    wordBlockFormat = 'p';
    wordLineHeight = '1.15';
    wordTableRows = 3;
    wordTableColumns = 3;
    private selectedWordTableCell: HTMLTableCellElement | null = null;
    private wordTableSelectionStart: HTMLTableCellElement | null = null;
    private wordTableSelectionEnd: HTMLTableCellElement | null = null;
    private isSelectingWordTableCells = false;
    private resizingWordTableColumn: { table: HTMLTableElement; colIndex: number; startX: number; startWidth: number } | null = null;
    private resizingColumnIndex: number | null = null;
    private resizeStartX = 0;
    private resizeStartWidth = 0;
    private univerDocsInstance?: UniverInstance;
    private univerDocsApi?: UniverApi;
    private univerClipboardHook?: { dispose: () => void };
    private univerPasteInterceptor?: () => void;

    canvasStyle: FormCanvasStyle = {
        background: '#ffffff',
        border: '#d8e0ea',
        accent: '#2f6fed',
        radius: 6,
        density: 'comfortable'
    };

    fontFamilies = ['Arial', 'Calibri', 'Cambria', 'Georgia', 'Times New Roman', 'Verdana'];
    fontSizes = [10, 11, 12, 14, 16, 18, 20, 24, 28, 32];
    wordBlockFormats = [
        { value: 'p', label: 'Normal' },
        { value: 'h1', label: 'Heading 1' },
        { value: 'h2', label: 'Heading 2' },
        { value: 'h3', label: 'Heading 3' },
        { value: 'blockquote', label: 'Quote' },
        { value: 'pre', label: 'Code' }
    ];
    wordLineHeights = [
        { value: '1', label: 'Single' },
        { value: '1.15', label: '1.15' },
        { value: '1.5', label: '1.5' },
        { value: '2', label: 'Double' }
    ];
    cellFormats: { value: CellFormat; label: string }[] = [
        { value: 'general', label: 'General' },
        { value: 'currency', label: 'Currency' },
        { value: 'percent', label: 'Percent' },
        { value: 'date', label: 'Date' }
    ];

    constructor() {
        this.resizeSheet();
    }

    ngAfterViewInit(): void {
        if (this.activeMode === 'document') {
            setTimeout(() => this.initializeUniverDocs());
        }
    }

    ngOnDestroy(): void {
        this.destroyUniverDocs();
    }

    get selectedSheetCell(): SheetCell {
        return this.sheetData[this.selectedCell.row]?.[this.selectedCell.col] || this.createCell();
    }

    get columnHeaders(): string[] {
        return Array.from({ length: this.sheetColumnCount }, (_, index) => this.toColumnName(index));
    }

    get selectedCellName(): string {
        return `${this.toColumnName(this.selectedCell.col)}${this.selectedCell.row + 1}`;
    }

    get selectedRowHeight(): number {
        return this.rowHeights[this.selectedCell.row] || 38;
    }

    set selectedRowHeight(value: number) {
        this.rowHeights[this.selectedCell.row] = this.clamp(Number(value) || 38, 24, 240);
    }

    get selectedColumnWidth(): number {
        return this.columnWidths[this.selectedCell.col] || 130;
    }

    set selectedColumnWidth(value: number) {
        this.columnWidths[this.selectedCell.col] = this.clamp(Number(value) || 130, 60, 360);
        this.recalculateWrappedRowHeights();
    }

    get selectedColumnName(): string {
        return this.toColumnName(this.selectedCell.col);
    }

    get selectedRange(): { startRow: number; endRow: number; startCol: number; endCol: number } {
        return {
            startRow: Math.min(this.selectionStart.row, this.selectionEnd.row),
            endRow: Math.max(this.selectionStart.row, this.selectionEnd.row),
            startCol: Math.min(this.selectionStart.col, this.selectionEnd.col),
            endCol: Math.max(this.selectionStart.col, this.selectionEnd.col)
        };
    }

    get selectedRangeName(): string {
        const range = this.selectedRange;
        const start = `${this.toColumnName(range.startCol)}${range.startRow + 1}`;
        const end = `${this.toColumnName(range.endCol)}${range.endRow + 1}`;
        return start === end ? start : `${start}:${end}`;
    }

    get isSingleCellSelection(): boolean {
        const range = this.selectedRange;
        return range.startRow === range.endRow && range.startCol === range.endCol;
    }

    get isMultiCellSelection(): boolean {
        return !this.isSingleCellSelection;
    }

    get isWholeRowSelection(): boolean {
        const range = this.selectedRange;
        return range.startCol === 0 && range.endCol === this.sheetColumnCount - 1;
    }

    get isWholeColumnSelection(): boolean {
        const range = this.selectedRange;
        return range.startRow === 0 && range.endRow === this.sheetRowCount - 1;
    }

    get hasMergedSelection(): boolean {
        let hasMerged = false;
        this.forEachSelectedCell((cell) => {
            if (cell.rowSpan > 1 || cell.colSpan > 1) {
                hasMerged = true;
            }
        });
        return hasMerged;
    }

    get hasSelectedWordTableCell(): boolean {
        return !!this.selectedWordTableCell;
    }

    get hasSelectedWordTableRange(): boolean {
        const range = this.getWordTableSelectionRange();
        return !!range && (range.startRow !== range.endRow || range.startCol !== range.endCol);
    }

    get canvasStyleVars(): Record<string, string> {
        return {
            '--builder-bg': this.canvasStyle.background,
            '--builder-border': this.canvasStyle.border,
            '--builder-accent': this.canvasStyle.accent,
            '--builder-radius': `${this.canvasStyle.radius}px`
        };
    }

    switchMode(mode: BuilderMode): void {
        this.activeMode = mode;
        this.closeContextMenu();
        if (mode === 'document') {
            setTimeout(() => this.initializeUniverDocs());
        } else {
            this.destroyUniverDocs();
        }
    }

    setRibbonTab(tab: RibbonTab): void {
        this.activeRibbonTab = tab;
    }

    setWordOrientation(orientation: PageOrientation): void {
        this.pageOrientation = orientation;
        void this.applyUniverPageSetup();
    }

    insertUniverDocTable(): void {
        void this.executeUniverDocCommand('doc.command.create-table', {
            rowCount: this.clamp(Number(this.wordTableRows) || 3, 1, 20),
            colCount: this.clamp(Number(this.wordTableColumns) || 3, 1, 12)
        });
    }

    insertUniverDocTableRow(position: 'above' | 'below'): void {
        void this.executeUniverDocCommand(
            position === 'above' ? 'doc.command.table-insert-row-above' : 'doc.command.table-insert-row-bellow'
        );
    }

    insertUniverDocTableColumn(position: 'left' | 'right'): void {
        void this.executeUniverDocCommand(
            position === 'left' ? 'doc.command.table-insert-column-left' : 'doc.command.table-insert-column-right'
        );
    }

    deleteUniverDocTablePart(part: 'rows' | 'columns' | 'table'): void {
        const commandId = part === 'rows'
            ? 'doc.table.delete-rows'
            : part === 'columns'
                ? 'doc.table.delete-columns'
                : 'doc.table.delete-table';
        void this.executeUniverDocCommand(commandId);
    }

    updateDensity(density: Density): void {
        this.canvasStyle.density = density;
    }

    applyQuickTheme(theme: 'clean' | 'blue' | 'charcoal'): void {
        const themes: Record<string, FormCanvasStyle> = {
            clean: { background: '#ffffff', border: '#d8e0ea', accent: '#2f6fed', radius: 6, density: this.canvasStyle.density },
            blue: { background: '#f8fbff', border: '#bdd3f8', accent: '#1d5fd1', radius: 4, density: this.canvasStyle.density },
            charcoal: { background: '#f7f7f5', border: '#c8c8c2', accent: '#30343b', radius: 2, density: this.canvasStyle.density }
        };

        this.canvasStyle = themes[theme];
    }

    resizeSheet(): void {
        this.unmergeAllCells();
        const rows = this.clamp(Number(this.sheetRowCount) || 1, 1, 100);
        const cols = this.clamp(Number(this.sheetColumnCount) || 1, 1, 26);
        this.sheetRowCount = rows;
        this.sheetColumnCount = cols;

        const nextData: SheetCell[][] = [];
        for (let row = 0; row < rows; row++) {
            nextData[row] = [];
            for (let col = 0; col < cols; col++) {
                nextData[row][col] = this.sheetData[row]?.[col] || this.createCell();
            }
        }
        this.sheetData = nextData;
        this.rowHeights = Array.from({ length: rows }, (_, row) => this.rowHeights[row] || 38);
        this.columnWidths = Array.from({ length: cols }, (_, col) => this.columnWidths[col] || 130);
        this.selectedCell.row = Math.min(this.selectedCell.row, rows - 1);
        this.selectedCell.col = Math.min(this.selectedCell.col, cols - 1);
        this.resetSelection(this.selectedCell.row, this.selectedCell.col);
        this.freezeRowCount = this.clamp(this.freezeRowCount, 0, rows);
        this.freezeColumnCount = this.clamp(this.freezeColumnCount, 0, cols);
    }

    selectCell(row: number, col: number, extend = false): void {
        const cell = this.sheetData[row]?.[col];
        const target = cell?.hidden && cell.masterRow != null && cell.masterCol != null
            ? { row: cell.masterRow, col: cell.masterCol }
            : { row, col };
        this.selectedCell = target;
        if (extend) {
            this.selectionEnd = target;
            return;
        }
        this.resetSelection(target.row, target.col);
    }

    beginSelection(event: MouseEvent, row: number, col: number): void {
        if (event.button !== 0) {
            event.preventDefault();
            return;
        }
        this.closeContextMenu();
        this.isSelecting = true;
        this.selectCell(row, col);
    }

    extendSelection(row: number, col: number): void {
        if (!this.isSelecting) {
            return;
        }
        this.selectCell(row, col, true);
    }

    endSelection(): void {
        this.isSelecting = false;
    }

    selectRow(row: number): void {
        this.selectedCell = { row, col: 0 };
        this.selectionStart = { row, col: 0 };
        this.selectionEnd = { row, col: this.sheetColumnCount - 1 };
        this.closeContextMenu();
    }

    selectColumn(col: number): void {
        this.selectedCell = { row: 0, col };
        this.selectionStart = { row: 0, col };
        this.selectionEnd = { row: this.sheetRowCount - 1, col };
        this.closeContextMenu();
    }

    isCellSelected(row: number, col: number): boolean {
        const range = this.selectedRange;
        return row >= range.startRow && row <= range.endRow && col >= range.startCol && col <= range.endCol;
    }

    isRowSelected(row: number): boolean {
        const range = this.selectedRange;
        return row >= range.startRow && row <= range.endRow && range.startCol === 0 && range.endCol === this.sheetColumnCount - 1;
    }

    isColumnSelected(col: number): boolean {
        const range = this.selectedRange;
        return col >= range.startCol && col <= range.endCol && range.startRow === 0 && range.endRow === this.sheetRowCount - 1;
    }

    insertRow(position: 'above' | 'below'): void {
        this.unmergeAllCells();
        const index = position === 'above' ? this.selectedCell.row : this.selectedCell.row + 1;
        this.sheetData.splice(index, 0, this.createRow());
        this.rowHeights.splice(index, 0, 38);
        this.sheetRowCount = this.sheetData.length;
        this.selectedCell = { row: index, col: this.selectedCell.col };
        this.resetSelection(index, this.selectedCell.col);
    }

    insertColumn(position: 'left' | 'right'): void {
        this.unmergeAllCells();
        const index = position === 'left' ? this.selectedCell.col : this.selectedCell.col + 1;
        this.sheetData.forEach((row) => row.splice(index, 0, this.createCell()));
        this.columnWidths.splice(index, 0, 130);
        this.sheetColumnCount = this.sheetData[0]?.length || 1;
        this.selectedCell = { row: this.selectedCell.row, col: index };
        this.resetSelection(this.selectedCell.row, index);
    }

    deleteSelectedRow(): void {
        if (this.sheetData.length <= 1) {
            return;
        }

        this.unmergeAllCells();
        this.sheetData.splice(this.selectedCell.row, 1);
        this.rowHeights.splice(this.selectedCell.row, 1);
        this.sheetRowCount = this.sheetData.length;
        this.selectedCell.row = Math.min(this.selectedCell.row, this.sheetRowCount - 1);
        this.resetSelection(this.selectedCell.row, this.selectedCell.col);
    }

    deleteSelectedColumn(): void {
        if ((this.sheetData[0]?.length || 0) <= 1) {
            return;
        }

        this.unmergeAllCells();
        this.sheetData.forEach((row) => row.splice(this.selectedCell.col, 1));
        this.columnWidths.splice(this.selectedCell.col, 1);
        this.sheetColumnCount = this.sheetData[0]?.length || 1;
        this.selectedCell.col = Math.min(this.selectedCell.col, this.sheetColumnCount - 1);
        this.resetSelection(this.selectedCell.row, this.selectedCell.col);
    }

    sortSelectedColumn(direction: 'asc' | 'desc'): void {
        this.unmergeAllCells();
        const col = this.selectedCell.col;
        const headerRows = this.freezeRowCount > 0 ? this.freezeRowCount : 0;
        const frozen = this.sheetData.slice(0, headerRows);
        const sortable = this.sheetData.slice(headerRows);
        sortable.sort((a, b) => {
            const left = this.getCellDisplayValue(a[col] || this.createCell()).toString();
            const right = this.getCellDisplayValue(b[col] || this.createCell()).toString();
            const leftNum = Number(left);
            const rightNum = Number(right);
            const result = Number.isFinite(leftNum) && Number.isFinite(rightNum)
                ? leftNum - rightNum
                : left.localeCompare(right);
            return direction === 'asc' ? result : -result;
        });
        this.sheetData = [...frozen, ...sortable];
    }

    shouldShowRow(row: SheetCell[], rowIndex: number): boolean {
        if (!this.filterText.trim()) {
            return true;
        }
        if (rowIndex < this.freezeRowCount) {
            return true;
        }
        return row.some((cell) => this.getCellDisplayValue(cell).toString().toLowerCase().includes(this.filterText.trim().toLowerCase()));
    }

    clearSelectedCell(): void {
        const affectedRows = new Set<number>();
        this.forEachSelectedCell((cell) => {
            cell.value = '';
        }, affectedRows);
        this.recalculateWrappedRowHeights(affectedRows);
    }

    updateCellValue(row: number, col: number, value: string): void {
        if (!this.sheetData[row]?.[col]) {
            return;
        }

        this.sheetData[row][col].value = value;
        this.recalculateWrappedRowHeights(new Set([row]));
    }

    updateSelectedCellValue(value: string): void {
        this.updateCellValue(this.selectedCell.row, this.selectedCell.col, value);
    }

    clearSheet(): void {
        this.sheetData = this.sheetData.map((row) => row.map(() => this.createCell()));
        this.filterText = '';
    }

    async copySheetToClipboard(): Promise<void> {
        const text = this.sheetData
            .map((row) => row.map((cell) => cell.value).join('\t'))
            .join('\n');
        await navigator.clipboard?.writeText(text);
    }

    async copySelectionToClipboard(): Promise<void> {
        const range = this.selectedRange;
        const rows: string[] = [];
        for (let row = range.startRow; row <= range.endRow; row++) {
            const values: string[] = [];
            for (let col = range.startCol; col <= range.endCol; col++) {
                values.push(this.sheetData[row]?.[col]?.value || '');
            }
            rows.push(values.join('\t'));
        }
        await navigator.clipboard?.writeText(rows.join('\n'));
    }

    async pasteClipboardIntoSheet(): Promise<void> {
        const text = await navigator.clipboard?.readText();
        if (!text) {
            return;
        }
        this.pasteTsv(text);
    }

    pasteTsv(text: string): void {
        const rows = text.split(/\r?\n/).filter((line) => line.length > 0).map((line) => line.split('\t'));
        const affectedRows = new Set<number>();
        rows.forEach((row, rowOffset) => {
            row.forEach((value, colOffset) => {
                const rowIndex = this.selectedCell.row + rowOffset;
                const colIndex = this.selectedCell.col + colOffset;
                if (this.sheetData[rowIndex]?.[colIndex]) {
                    this.sheetData[rowIndex][colIndex].value = value;
                    affectedRows.add(rowIndex);
                }
            });
        });
        this.recalculateWrappedRowHeights(affectedRows);
    }

    exportCsv(): void {
        const csv = this.sheetData
            .map((row) => row.map((cell) => `"${cell.value.replace(/"/g, '""')}"`).join(','))
            .join('\n');
        const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = 'univer-form-builder-sheet.csv';
        link.click();
        URL.revokeObjectURL(url);
    }

    mergeSelectedCells(): void {
        const range = this.selectedRange;
        const hasRange = range.startRow !== range.endRow || range.startCol !== range.endCol;
        const startRow = range.startRow;
        const startCol = range.startCol;
        const rowSpan = hasRange
            ? range.endRow - range.startRow + 1
            : this.clamp(Number(this.mergeRowCount) || 1, 1, this.sheetRowCount - startRow);
        const colSpan = hasRange
            ? range.endCol - range.startCol + 1
            : this.clamp(Number(this.mergeColumnCount) || 1, 1, this.sheetColumnCount - startCol);

        if (rowSpan === 1 && colSpan === 1) {
            return;
        }

        this.unmergeRange(startRow, startCol, rowSpan, colSpan);

        const master = this.sheetData[startRow][startCol];
        master.rowSpan = rowSpan;
        master.colSpan = colSpan;
        master.hidden = false;

        for (let row = startRow; row < startRow + rowSpan; row++) {
            for (let col = startCol; col < startCol + colSpan; col++) {
                if (row === startRow && col === startCol) {
                    continue;
                }

                const cell = this.sheetData[row][col];
                if (cell.value && !master.value) {
                    master.value = cell.value;
                }
                cell.hidden = true;
                cell.masterRow = startRow;
                cell.masterCol = startCol;
                cell.rowSpan = 1;
                cell.colSpan = 1;
            }
        }

        this.mergeRowCount = rowSpan;
        this.mergeColumnCount = colSpan;
        this.selectedCell = { row: startRow, col: startCol };
        this.selectionStart = { row: startRow, col: startCol };
        this.selectionEnd = { row: startRow + rowSpan - 1, col: startCol + colSpan - 1 };
    }

    unmergeSelectedCell(): void {
        const cell = this.selectedSheetCell;
        this.unmergeRange(this.selectedCell.row, this.selectedCell.col, cell.rowSpan, cell.colSpan);
    }

    unmergeSelectedCells(): void {
        const range = this.selectedRange;
        for (let row = range.startRow; row <= range.endRow; row++) {
            for (let col = range.startCol; col <= range.endCol; col++) {
                const cell = this.sheetData[row]?.[col];
                if (cell && !cell.hidden && (cell.rowSpan > 1 || cell.colSpan > 1)) {
                    this.unmergeRange(row, col, cell.rowSpan, cell.colSpan);
                }
            }
        }
    }

    toggleStyle(style: 'bold' | 'italic' | 'underline'): void {
        if (this.activeMode === 'document') {
            this.exec(style === 'bold' ? 'bold' : style === 'italic' ? 'italic' : 'underline');
            return;
        }

        const nextValue = !this.selectedSheetCell[style];
        this.forEachSelectedCell((cell) => {
            cell[style] = nextValue;
        });
    }

    toggleWrap(): void {
        const nextValue = !this.selectedSheetCell.wrap;
        const affectedRows = new Set<number>();
        this.forEachSelectedCell((cell) => {
            cell.wrap = nextValue;
        }, affectedRows);
        this.recalculateWrappedRowHeights(affectedRows);
    }

    setAlignment(textAlign: TextAlign): void {
        if (this.activeMode === 'document') {
            this.exec(textAlign === 'left' ? 'justifyLeft' : textAlign === 'center' ? 'justifyCenter' : 'justifyRight');
            return;
        }

        this.forEachSelectedCell((cell) => {
            cell.textAlign = textAlign;
        });
    }

    increaseFontSize(): void {
        if (this.activeMode === 'document') {
            this.wordFontSize = Math.min(72, this.wordFontSize + 1);
            this.applyWordFontSize();
            return;
        }

        const nextSize = Math.min(72, this.selectedSheetCell.fontSize + 1);
        this.forEachSelectedCell((cell) => {
            cell.fontSize = nextSize;
        });
    }

    decreaseFontSize(): void {
        if (this.activeMode === 'document') {
            this.wordFontSize = Math.max(8, this.wordFontSize - 1);
            this.applyWordFontSize();
            return;
        }

        const nextSize = Math.max(8, this.selectedSheetCell.fontSize - 1);
        this.forEachSelectedCell((cell) => {
            cell.fontSize = nextSize;
        });
    }

    applyFontFamily(fontFamily: string): void {
        if (this.activeMode === 'document') {
            this.wordFontFamily = fontFamily;
            this.exec('fontName', fontFamily);
            return;
        }

        this.forEachSelectedCell((cell) => {
            cell.fontFamily = fontFamily;
        });
    }

    applyFontSize(fontSize: number): void {
        if (this.activeMode === 'document') {
            this.wordFontSize = Number(fontSize);
            this.applyWordFontSize();
            return;
        }

        const nextSize = Number(fontSize);
        this.forEachSelectedCell((cell) => {
            cell.fontSize = nextSize;
        });
    }

    applyTextColor(color: string): void {
        if (this.activeMode === 'document') {
            this.exec('foreColor', color);
            return;
        }

        this.forEachSelectedCell((cell) => {
            cell.color = color;
        });
    }

    applyFillColor(color: string): void {
        if (this.activeMode === 'document') {
            this.exec('hiliteColor', color);
            return;
        }

        this.forEachSelectedCell((cell) => {
            cell.background = color;
        });
    }

    applyCellFormat(format: CellFormat): void {
        this.forEachSelectedCell((cell) => {
            cell.format = format;
        });
    }

    getCellStyle(cell: SheetCell): Record<string, string> {
        return {
            'font-family': cell.fontFamily,
            'font-size': `${cell.fontSize}px`,
            'font-weight': cell.bold ? '700' : '400',
            'font-style': cell.italic ? 'italic' : 'normal',
            'text-decoration': cell.underline ? 'underline' : 'none',
            'text-align': cell.textAlign,
            color: cell.color,
            background: cell.background,
            'border-color': cell.borderColor,
            'white-space': cell.wrap ? 'normal' : 'nowrap'
        };
    }

    getCellDisplayValue(cell: SheetCell): string | number {
        const raw = cell.value || '';
        const calculated = raw.startsWith('=') ? this.evaluateFormula(raw) : raw;

        if (cell.format === 'currency') {
            const num = Number(calculated);
            return Number.isFinite(num) ? num.toLocaleString(undefined, { style: 'currency', currency: 'USD' }) : calculated;
        }
        if (cell.format === 'percent') {
            const num = Number(calculated);
            return Number.isFinite(num) ? `${(num * 100).toFixed(2)}%` : calculated;
        }
        if (cell.format === 'date') {
            const date = new Date(calculated);
            return Number.isNaN(date.getTime()) ? calculated : date.toLocaleDateString();
        }
        return calculated;
    }

    getColumnStyle(colIndex: number): Record<string, string> {
        const width = this.columnWidths[colIndex] || 130;
        return { width: `${width}px`, 'min-width': `${width}px`, 'max-width': `${width}px` };
    }

    getRowStyle(rowIndex: number): Record<string, string> {
        const height = this.rowHeights[rowIndex] || 38;
        return { height: `${height}px` };
    }

    isFrozenRow(rowIndex: number): boolean {
        return rowIndex < this.freezeRowCount;
    }

    isFrozenColumn(colIndex: number): boolean {
        return colIndex < this.freezeColumnCount;
    }

    getFrozenColumnLeft(colIndex: number): number {
        if (!this.isFrozenColumn(colIndex)) {
            return 0;
        }
        return 52 + this.columnWidths.slice(0, colIndex).reduce((sum, width) => sum + width, 0);
    }

    getFrozenRowTop(rowIndex: number): number {
        if (!this.isFrozenRow(rowIndex)) {
            return 0;
        }
        return 38 + this.rowHeights.slice(0, rowIndex).reduce((sum, height) => sum + height, 0);
    }

    startColumnResize(event: MouseEvent, colIndex: number): void {
        event.preventDefault();
        event.stopPropagation();
        this.resizingColumnIndex = colIndex;
        this.resizeStartX = event.clientX;
        this.resizeStartWidth = this.columnWidths[colIndex] || 130;
        document.addEventListener('mousemove', this.onColumnResize);
        document.addEventListener('mouseup', this.stopColumnResize);
    }

    private onColumnResize = (event: MouseEvent): void => {
        if (this.resizingColumnIndex === null) {
            return;
        }
        const delta = event.clientX - this.resizeStartX;
        this.columnWidths[this.resizingColumnIndex] = this.clamp(this.resizeStartWidth + delta, 60, 360);
        this.recalculateWrappedRowHeights();
    };

    private stopColumnResize = (): void => {
        this.resizingColumnIndex = null;
        document.removeEventListener('mousemove', this.onColumnResize);
        document.removeEventListener('mouseup', this.stopColumnResize);
    };

    exec(command: string, value?: string): void {
        this.wordEditor?.nativeElement.focus();
        document.execCommand(command, false, value);
    }

    applyWordBlockFormat(format: string): void {
        this.wordBlockFormat = format;
        this.exec('formatBlock', format);
    }

    applyWordLineHeight(lineHeight: string): void {
        this.wordLineHeight = lineHeight;
        this.wrapSelectionWithStyle(`line-height: ${lineHeight};`);
    }

    clearWordFormatting(): void {
        this.exec('removeFormat');
        this.exec('formatBlock', 'p');
    }

    insertWordLink(): void {
        const url = window.prompt('Enter link URL');
        if (!url) {
            return;
        }

        this.exec('createLink', url);
    }

    insertWordImage(): void {
        const url = window.prompt('Enter image URL');
        if (!url) {
            return;
        }

        this.exec('insertHTML', `<img src="${this.escapeHtml(url)}" alt="" />`);
    }

    insertWordTable(rows = this.wordTableRows, columns = this.wordTableColumns): void {
        rows = this.clamp(Number(rows) || 3, 1, 20);
        columns = this.clamp(Number(columns) || 3, 1, 12);
        const colGroup = [
            '<col class="word-sheet-row-header-col" />',
            ...Array.from({ length: columns }, () => '<col class="word-sheet-data-col" />')
        ].join('');
        const headerCells = Array.from({ length: columns }, (_, index) => this.getWordColumnHeaderHtml(index)).join('');
        const tableRows = Array.from({ length: rows }, (_, rowIndex) => {
            const cells = Array.from({ length: columns }, () => '<td><br></td>').join('');
            return `<tr><th contenteditable="false">${rowIndex + 1}</th>${cells}</tr>`;
        }).join('');
        this.exec('insertHTML', `<table class="word-table word-sheet-table"><colgroup>${colGroup}</colgroup><thead><tr><th contenteditable="false"></th>${headerCells}</tr></thead><tbody>${tableRows}</tbody></table><p><br></p>`);
    }

    insertWordDate(): void {
        this.exec('insertText', new Date().toLocaleDateString());
    }

    insertWordSignatureLine(): void {
        this.exec('insertHTML', '<p class="signature-line"><br></p>');
    }

    insertWordPageBreak(): void {
        this.exec('insertHTML', '<div class="word-page-break" contenteditable="false"><span>Page Break</span></div><p><br></p>');
    }

    trackWordSelection(event?: Event): void {
        const target = event?.target as HTMLElement | null;
        const targetCell = target?.closest?.('td') as HTMLTableCellElement | null;
        if (targetCell && this.wordEditor?.nativeElement.contains(targetCell)) {
            this.setSelectedWordTableCell(targetCell);
            return;
        }

        const selection = window.getSelection();
        const anchor = selection?.anchorNode;
        const anchorElement = anchor instanceof HTMLElement ? anchor : anchor?.parentElement || null;
        const selectionCell = anchorElement?.closest?.('td') as HTMLTableCellElement | null;
        if (selectionCell && this.wordEditor?.nativeElement.contains(selectionCell)) {
            this.setSelectedWordTableCell(selectionCell);
        }
    }

    handleWordEditorMouseDown(event: MouseEvent): void {
        const target = event.target as HTMLElement | null;
        const resizeHandle = target?.closest?.('.word-column-resize-handle') as HTMLElement | null;
        if (resizeHandle) {
            this.startWordTableColumnResize(event, resizeHandle);
            return;
        }

        const cell = target?.closest?.('td') as HTMLTableCellElement | null;
        if (!cell || !this.wordEditor?.nativeElement.contains(cell)) {
            return;
        }

        this.isSelectingWordTableCells = true;
        this.wordTableSelectionStart = cell;
        this.wordTableSelectionEnd = cell;
        this.setSelectedWordTableCell(cell);
        this.highlightWordTableSelection();
    }

    handleWordEditorMouseOver(event: MouseEvent): void {
        if (!this.isSelectingWordTableCells) {
            return;
        }

        const cell = (event.target as HTMLElement | null)?.closest?.('td') as HTMLTableCellElement | null;
        if (!cell || cell.closest('table') !== this.wordTableSelectionStart?.closest('table')) {
            return;
        }

        this.wordTableSelectionEnd = cell;
        this.setSelectedWordTableCell(cell);
        this.highlightWordTableSelection();
    }

    handleWordEditorMouseUp(): void {
        this.isSelectingWordTableCells = false;
    }

    insertWordTableRow(position: 'above' | 'below'): void {
        const cell = this.getSelectedWordTableCell();
        const row = cell?.parentElement as HTMLTableRowElement | null;
        if (!cell || !row) {
            return;
        }

        const newRow = row.cloneNode(true) as HTMLTableRowElement;
        Array.from(newRow.cells).forEach((newCell) => {
            newCell.classList.remove('selected-word-table-cell');
            if (newCell.tagName.toLowerCase() === 'td') {
                newCell.innerHTML = '<br>';
            }
        });
        row.parentElement?.insertBefore(newRow, position === 'above' ? row : row.nextSibling);
        this.updateWordSheetTableHeaders(row.closest('table'));
        this.setSelectedWordTableCell(newRow.cells[Math.min(cell.cellIndex, newRow.cells.length - 1)] as HTMLTableCellElement);
    }

    insertWordTableColumn(position: 'left' | 'right'): void {
        const cell = this.getSelectedWordTableCell();
        const table = cell?.closest('table');
        if (!cell || !table) {
            return;
        }

        const insertIndex = position === 'left' ? cell.cellIndex : cell.cellIndex + 1;
        const headerRow = table.tHead?.rows[0];
        if (headerRow) {
            const headerCell = document.createElement('th');
            headerCell.contentEditable = 'false';
            headerRow.insertBefore(headerCell, headerRow.cells[insertIndex] || null);
        }
        const colGroup = table.querySelector('colgroup');
        if (colGroup) {
            const col = document.createElement('col');
            col.className = 'word-sheet-data-col';
            colGroup.insertBefore(col, colGroup.children[insertIndex] || null);
        }
        Array.from(table.tBodies[0]?.rows || []).forEach((row) => {
            const newCell = row.insertCell(insertIndex);
            newCell.innerHTML = '<br>';
        });
        this.updateWordSheetTableHeaders(table);
        this.setSelectedWordTableCell((cell.parentElement as HTMLTableRowElement).cells[insertIndex] as HTMLTableCellElement);
    }

    mergeSelectedWordTableCells(): void {
        const range = this.getWordTableSelectionRange();
        if (!range || (range.startRow === range.endRow && range.startCol === range.endCol)) {
            return;
        }

        const master = range.table.rows[range.startRow]?.cells[range.startCol] as HTMLTableCellElement | undefined;
        if (!master || master.tagName.toLowerCase() !== 'td') {
            return;
        }

        const values: string[] = [];
        for (let rowIndex = range.startRow; rowIndex <= range.endRow; rowIndex++) {
            for (let colIndex = range.startCol; colIndex <= range.endCol; colIndex++) {
                const cell = range.table.rows[rowIndex]?.cells[colIndex] as HTMLTableCellElement | undefined;
                if (cell?.innerText.trim()) {
                    values.push(cell.innerText.trim());
                }
            }
        }

        for (let rowIndex = range.endRow; rowIndex >= range.startRow; rowIndex--) {
            for (let colIndex = range.endCol; colIndex >= range.startCol; colIndex--) {
                if (rowIndex === range.startRow && colIndex === range.startCol) {
                    continue;
                }
                range.table.rows[rowIndex]?.cells[colIndex]?.remove();
            }
        }

        master.rowSpan = range.endRow - range.startRow + 1;
        master.colSpan = range.endCol - range.startCol + 1;
        if (values.length && !master.innerText.trim()) {
            master.textContent = values.join(' ');
        }
        this.wordTableSelectionStart = master;
        this.wordTableSelectionEnd = master;
        this.setSelectedWordTableCell(master);
        this.highlightWordTableSelection();
    }

    unmergeSelectedWordTableCell(): void {
        const cell = this.getSelectedWordTableCell();
        const row = cell?.parentElement as HTMLTableRowElement | null;
        if (!cell || !row || (cell.rowSpan <= 1 && cell.colSpan <= 1)) {
            return;
        }

        const rowSpan = cell.rowSpan;
        const colSpan = cell.colSpan;
        const startCol = cell.cellIndex;
        cell.rowSpan = 1;
        cell.colSpan = 1;

        for (let rowOffset = 0; rowOffset < rowSpan; rowOffset++) {
            const targetRow = row.parentElement?.children[row.sectionRowIndex + rowOffset] as HTMLTableRowElement | undefined;
            if (!targetRow) {
                continue;
            }
            const cellsToAdd = rowOffset === 0 ? colSpan - 1 : colSpan;
            const insertBefore = targetRow.cells[rowOffset === 0 ? startCol + 1 : startCol] || null;
            for (let index = 0; index < cellsToAdd; index++) {
                const newCell = document.createElement('td');
                newCell.innerHTML = '<br>';
                targetRow.insertBefore(newCell, insertBefore);
            }
        }

        this.setSelectedWordTableCell(cell);
        this.highlightWordTableSelection();
    }

    deleteWordTableRow(): void {
        const cell = this.getSelectedWordTableCell();
        const row = cell?.parentElement as HTMLTableRowElement | null;
        const table = cell?.closest('table');
        if (!cell || !row || !table) {
            return;
        }

        if (table.rows.length <= 1) {
            table.remove();
            this.selectedWordTableCell = null;
            return;
        }

        const nextRow = row.nextElementSibling || row.previousElementSibling;
        const cellIndex = cell.cellIndex;
        row.remove();
        this.updateWordSheetTableHeaders(table);
        const nextCell = nextRow ? (nextRow as HTMLTableRowElement).cells[Math.min(cellIndex, (nextRow as HTMLTableRowElement).cells.length - 1)] : null;
        this.setSelectedWordTableCell(nextCell as HTMLTableCellElement | null);
    }

    deleteWordTableColumn(): void {
        const cell = this.getSelectedWordTableCell();
        const table = cell?.closest('table');
        if (!cell || !table) {
            return;
        }

        const cellIndex = cell.cellIndex;
        const rowIndex = (cell.parentElement as HTMLTableRowElement).rowIndex;
        const columnCount = table.rows[0]?.cells.length || 0;
        if (columnCount <= 2) {
            table.remove();
            this.selectedWordTableCell = null;
            return;
        }

        Array.from(table.rows).forEach((row) => row.deleteCell(cellIndex));
        const colGroup = table.querySelector('colgroup');
        colGroup?.children[cellIndex]?.remove();
        this.updateWordSheetTableHeaders(table);
        const nextRow = table.rows[Math.min(rowIndex, table.rows.length - 1)];
        const nextCell = nextRow?.cells[Math.min(cellIndex, nextRow.cells.length - 1)] || null;
        this.setSelectedWordTableCell(nextCell as HTMLTableCellElement | null);
    }

    openCellContextMenu(event: MouseEvent, row: number, col: number): void {
        event.preventDefault();
        if (!this.isCellSelected(row, col)) {
            this.selectCell(row, col);
        }
        this.openContextMenu(event, 'cell');
    }

    openRowContextMenu(event: MouseEvent, row: number): void {
        event.preventDefault();
        this.selectRow(row);
        this.openContextMenu(event, 'row');
    }

    openColumnContextMenu(event: MouseEvent, col: number): void {
        event.preventDefault();
        this.selectColumn(col);
        this.openContextMenu(event, 'column');
    }

    closeContextMenu(): void {
        this.contextMenu.visible = false;
    }

    private initializeUniverDocs(): void {
        const host = this.univerDocsHost?.nativeElement;
        if (!host || this.univerDocsInstance) {
            return;
        }

        const { univer, univerAPI } = createUniver({
            locale: LocaleType.EN_US,
            locales: {
                [LocaleType.EN_US]: mergeLocales(UniverPresetDocsCoreEnUS),
            },
            presets: [
                UniverDocsCorePreset({
                    container: host,
                    header: true,
                    toolbar: true,
                    footer: true,
                    contextMenu: true,
                    ribbonType: 'classic',
                }),
            ],
        });

        this.univerDocsApi = univerAPI as unknown as UniverApi;
        this.univerDocsInstance = univer as unknown as UniverInstance;
        this.univerDocsApi.createDocument({
            title: 'Univer Form Builder',
            documentStyle: this.getUniverDocumentStyle(),
            settings: {
                zoomRatio: 1
            }
        });
        this.registerWordPasteNormalizer();
        this.registerUniverTableMergeMenu();
        setTimeout(() => void this.applyUniverPageSetup());
    }

    private destroyUniverDocs(): void {
        this.univerClipboardHook?.dispose();
        this.univerClipboardHook = undefined;
        this.univerPasteInterceptor?.();
        this.univerPasteInterceptor = undefined;
        this.univerDocsInstance?.dispose();
        this.univerDocsInstance = undefined;
        this.univerDocsApi = undefined;
        if (this.univerDocsHost?.nativeElement) {
            this.univerDocsHost.nativeElement.innerHTML = '';
        }
    }

    private async applyUniverPageSetup(): Promise<void> {
        await this.executeUniverDocCommand('docs.command.page-setup', this.getUniverDocumentStyle());
    }

    private async executeUniverDocCommand<P extends object = object>(commandId: string, params?: P): Promise<void> {
        if (!this.univerDocsApi) {
            return;
        }

        try {
            await this.univerDocsApi.executeCommand(commandId, params);
        } catch (error) {
            console.error(`Unable to execute Univer Docs command: ${commandId}`, error);
        }
    }

    private async mergeUniverTableSelection(): Promise<void> {
        const range = this.getSelectedUniverTableRange();
        const api = this.univerDocsApi;
        const injector = this.univerDocsInstance?.__getInjector();
        if (!range || !api || !injector) {
            return;
        }

        const instanceService = injector.get<any>(IUniverInstanceService);
        const docDataModel = instanceService.getCurrentUnitOfType(UniverInstanceType.UNIVER_DOC);
        const snapshot = docDataModel?.getSnapshot?.();
        const table = snapshot?.tableSource?.[range.tableId];
        if (!docDataModel || !snapshot || !table) {
            return;
        }

        const jsonX = JSONX.getInstance();
        const rawActions: unknown[] = [];
        const rowSpan = range.endRow - range.startRow + 1;
        const columnSpan = range.endColumn - range.startColumn + 1;

        for (let row = range.startRow; row <= range.endRow; row++) {
            for (let column = range.startColumn; column <= range.endColumn; column++) {
                const oldCell = table.tableRows?.[row]?.tableCells?.[column];
                if (!oldCell) {
                    continue;
                }

                const nextCell = {
                    ...oldCell,
                    rowSpan: row === range.startRow && column === range.startColumn ? rowSpan : 0,
                    columnSpan: row === range.startRow && column === range.startColumn ? columnSpan : 0
                };

                rawActions.push(jsonX.replaceOp([
                    'tableSource',
                    range.tableId,
                    'tableRows',
                    row,
                    'tableCells',
                    column
                ], oldCell, nextCell));
            }
        }

        const actions = rawActions.reduce((acc, action) => JSONX.compose(acc as never, action as never), null as never);
        await api.executeCommand(RichTextEditingMutation.id, {
            unitId: docDataModel.getUnitId(),
            actions,
            textRanges: [],
            segmentId: range.segmentId,
            historyAction: 'edit-table-cell',
            trigger: 'doc.command.table-merge-cells'
        });
    }

    private registerUniverTableMergeMenu(): void {
        this.univerDocsApi?.createMenu?.({
            id: 'doc.command.table-merge-cells',
            title: 'Merge Cells',
            tooltip: 'Merge selected table cells',
            order: -10,
            action: () => {
                void this.mergeUniverTableSelection();
            }
        }).appendTo('contextMenu.others');
    }

    private registerWordPasteNormalizer(): void {
        const injector = this.univerDocsInstance?.__getInjector();
        if (!injector) {
            return;
        }

        const clipboardService = injector.get<any>(IDocClipboardService);
        this.univerClipboardHook = clipboardService.addClipboardHook({
            onBeforePaste: (body: any, context: { documentData: { tableSource?: Record<string, any> } }) => {
                this.matchParagraphWidthToPastedTables(body, context.documentData.tableSource || {});
                return body;
            }
        });

        this.registerWordPasteHtmlNormalizer(clipboardService);
    }

    private matchParagraphWidthToPastedTables(body: any, tableSource: Record<string, any>): void {
        const pastedTables = body?.tables || [];
        const paragraphs = body?.paragraphs || [];
        if (!pastedTables.length || !paragraphs.length) {
            return;
        }

        const documentStyle = this.getUniverDocumentStyle();
        const contentWidth = documentStyle.pageSize.width - documentStyle.marginLeft - documentStyle.marginRight;
        const widestTableWidth = pastedTables.reduce((widest: number, tableRange: { tableId: string }) => {
            const table = tableSource[tableRange.tableId];
            const width = (table?.tableColumns || []).reduce((sum: number, column: any) => sum + (column?.size?.width?.v || 0), 0);
            return Math.max(widest, width);
        }, 0);
        const paragraphRightInset = Math.max(0, Math.round(contentWidth - widestTableWidth));

        if (!paragraphRightInset) {
            return;
        }

        const tableRanges = pastedTables.map((table: { startIndex: number; endIndex: number }) => ({
            startIndex: table.startIndex,
            endIndex: table.endIndex
        }));

        paragraphs.forEach((paragraph: any) => {
            const insideTable = tableRanges.some((table: { startIndex: number; endIndex: number }) =>
                table.startIndex <= paragraph.startIndex && paragraph.startIndex <= table.endIndex
            );
            if (insideTable) {
                return;
            }

            paragraph.paragraphStyle = {
                ...(paragraph.paragraphStyle || {}),
                indentEnd: { v: paragraphRightInset }
            };
        });
    }

    private registerWordPasteHtmlNormalizer(clipboardService: any): void {
        const host = this.univerDocsHost?.nativeElement;
        if (!host || this.univerPasteInterceptor) {
            return;
        }

        const onPaste = (event: ClipboardEvent) => {
            const clipboardData = event.clipboardData;
            const html = clipboardData?.getData('text/html') || '';
            if (!html || (!html.includes('urn:schemas-microsoft-com:office:word') && !html.includes('<table'))) {
                return;
            }

            const normalizedHtml = this.normalizeWordPasteHtml(html);
            if (normalizedHtml === html) {
                return;
            }

            event.preventDefault();
            event.stopPropagation();
            void clipboardService.legacyPaste({
                html: normalizedHtml,
                text: clipboardData?.getData('text/plain') || '',
                files: [],
                mode: 'source'
            });
        };

        host.addEventListener('paste', onPaste, true);
        this.univerPasteInterceptor = () => host.removeEventListener('paste', onPaste, true);
    }

    private normalizeWordPasteHtml(html: string): string {
        return html
            .replace(/LoanApplied/g, 'Loan Applied')
            .replace(/Constric(?:-|&shy;|\u00ad)\s*tion/gi, 'Construction')
            .replace(/Amount of Loan\s*Applied/g, 'Amount of Loan Applied');
    }

    private getSelectedUniverTableRange(): UniverTableRange | null {
        const injector = this.univerDocsInstance?.__getInjector();
        if (!injector) {
            return null;
        }

        const selectionService = injector.get<any>(DocSelectionManagerService);
        const rectRanges = selectionService.getRectRanges?.() || [];
        if (!rectRanges.length) {
            return null;
        }

        const tableId = rectRanges[0].tableId;
        if (!tableId || rectRanges.some((range: UniverTableRange) => range.tableId !== tableId)) {
            return null;
        }

        return rectRanges.reduce((acc: UniverTableRange, range: UniverTableRange) => ({
            tableId,
            startRow: Math.min(acc.startRow, range.startRow),
            endRow: Math.max(acc.endRow, range.endRow),
            startColumn: Math.min(acc.startColumn, range.startColumn),
            endColumn: Math.max(acc.endColumn, range.endColumn),
            segmentId: acc.segmentId || range.segmentId
        }), {
            tableId,
            startRow: rectRanges[0].startRow,
            endRow: rectRanges[0].endRow,
            startColumn: rectRanges[0].startColumn,
            endColumn: rectRanges[0].endColumn,
            segmentId: rectRanges[0].segmentId
        });
    }

    private getUniverDocumentStyle(): {
        documentFlavor: DocumentFlavor;
        pageOrient: PageOrientType;
        pageSize: { width: number; height: number };
        marginTop: number;
        marginBottom: number;
        marginLeft: number;
        marginRight: number;
    } {
        const a4Size = PAGE_SIZE[PaperType.A4];

        return {
            documentFlavor: DocumentFlavor.TRADITIONAL,
            pageOrient: this.pageOrientation === 'landscape' ? PageOrientType.LANDSCAPE : PageOrientType.PORTRAIT,
            pageSize: {
                width: a4Size.width,
                height: a4Size.height
            },
            marginTop: 24,
            marginBottom: 24,
            marginLeft: 24,
            marginRight: 24
        };
    }

    private applyWordFontSize(): void {
        this.wordEditor?.nativeElement.focus();
        document.execCommand('fontSize', false, '3');
        const fonts = this.wordEditor?.nativeElement.querySelectorAll('font[size="3"]');
        fonts?.forEach((font) => {
            (font as HTMLElement).removeAttribute('size');
            (font as HTMLElement).style.fontSize = `${this.wordFontSize}px`;
        });
    }

    private wrapSelectionWithStyle(style: string): void {
        this.wordEditor?.nativeElement.focus();
        const selection = window.getSelection();
        if (!selection || selection.rangeCount === 0 || selection.isCollapsed) {
            this.exec('insertHTML', `<p style="${style}"><br></p>`);
            return;
        }

        const range = selection.getRangeAt(0);
        const wrapper = document.createElement('span');
        wrapper.setAttribute('style', style);
        wrapper.appendChild(range.extractContents());
        range.insertNode(wrapper);
        selection.removeAllRanges();
        const nextRange = document.createRange();
        nextRange.selectNodeContents(wrapper);
        selection.addRange(nextRange);
    }

    private getSelectedWordTableCell(): HTMLTableCellElement | null {
        this.trackWordSelection();
        return this.selectedWordTableCell;
    }

    private setSelectedWordTableCell(cell: HTMLTableCellElement | null): void {
        this.wordEditor?.nativeElement.querySelectorAll('.selected-word-table-cell').forEach((selectedCell) => {
            selectedCell.classList.remove('selected-word-table-cell');
        });
        this.selectedWordTableCell = cell;
        this.selectedWordTableCell?.classList.add('selected-word-table-cell');
    }

    private highlightWordTableSelection(): void {
        this.wordEditor?.nativeElement.querySelectorAll('.selected-word-table-range-cell').forEach((cell) => {
            cell.classList.remove('selected-word-table-range-cell');
        });

        const range = this.getWordTableSelectionRange();
        if (!range) {
            return;
        }

        for (let rowIndex = range.startRow; rowIndex <= range.endRow; rowIndex++) {
            for (let colIndex = range.startCol; colIndex <= range.endCol; colIndex++) {
                const cell = range.table.rows[rowIndex]?.cells[colIndex];
                if (cell?.tagName.toLowerCase() === 'td') {
                    cell.classList.add('selected-word-table-range-cell');
                }
            }
        }
    }

    private getWordTableSelectionRange(): { table: HTMLTableElement; startRow: number; endRow: number; startCol: number; endCol: number } | null {
        const start = this.wordTableSelectionStart || this.selectedWordTableCell;
        const end = this.wordTableSelectionEnd || this.selectedWordTableCell;
        const table = start?.closest('table') as HTMLTableElement | null;
        if (!start || !end || !table || end.closest('table') !== table) {
            return null;
        }

        return {
            table,
            startRow: Math.min((start.parentElement as HTMLTableRowElement).rowIndex, (end.parentElement as HTMLTableRowElement).rowIndex),
            endRow: Math.max((start.parentElement as HTMLTableRowElement).rowIndex, (end.parentElement as HTMLTableRowElement).rowIndex),
            startCol: Math.min(start.cellIndex, end.cellIndex),
            endCol: Math.max(start.cellIndex, end.cellIndex)
        };
    }

    private startWordTableColumnResize(event: MouseEvent, handle: HTMLElement): void {
        event.preventDefault();
        event.stopPropagation();
        const table = handle.closest('table') as HTMLTableElement | null;
        const header = handle.closest('th') as HTMLTableCellElement | null;
        if (!table || !header) {
            return;
        }

        const colIndex = header.cellIndex;
        const col = table.querySelectorAll('col')[colIndex] as HTMLTableColElement | undefined;
        this.resizingWordTableColumn = {
            table,
            colIndex,
            startX: event.clientX,
            startWidth: col?.getBoundingClientRect().width || header.getBoundingClientRect().width || 130
        };
        document.addEventListener('mousemove', this.onWordTableColumnResize);
        document.addEventListener('mouseup', this.stopWordTableColumnResize);
    }

    private onWordTableColumnResize = (event: MouseEvent): void => {
        if (!this.resizingWordTableColumn) {
            return;
        }

        const width = this.clamp(this.resizingWordTableColumn.startWidth + event.clientX - this.resizingWordTableColumn.startX, 60, 360);
        const col = this.resizingWordTableColumn.table.querySelectorAll('col')[this.resizingWordTableColumn.colIndex] as HTMLTableColElement | undefined;
        if (col) {
            col.style.width = `${width}px`;
        }
    };

    private stopWordTableColumnResize = (): void => {
        this.resizingWordTableColumn = null;
        document.removeEventListener('mousemove', this.onWordTableColumnResize);
        document.removeEventListener('mouseup', this.stopWordTableColumnResize);
    };

    private getWordColumnHeaderHtml(index: number): string {
        return `<th contenteditable="false"><span>${this.toColumnName(index)}</span><button type="button" class="word-column-resize-handle" contenteditable="false"></button></th>`;
    }

    private updateWordSheetTableHeaders(table: HTMLTableElement | null): void {
        if (!table?.classList.contains('word-sheet-table')) {
            return;
        }

        const headerRow = table.tHead?.rows[0];
        if (headerRow) {
            Array.from(headerRow.cells).forEach((cell, index) => {
                cell.contentEditable = 'false';
                cell.innerHTML = index === 0 ? '' : `<span>${this.toColumnName(index - 1)}</span><button type="button" class="word-column-resize-handle" contenteditable="false"></button>`;
            });
        }

        Array.from(table.tBodies[0]?.rows || []).forEach((row, index) => {
            const header = row.cells[0];
            if (header) {
                header.contentEditable = 'false';
                header.textContent = String(index + 1);
            }
        });
    }

    private escapeHtml(value: string): string {
        return value
            .replace(/&/g, '&amp;')
            .replace(/"/g, '&quot;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;');
    }

    private createCell(): SheetCell {
        return {
            value: '',
            format: 'general',
            rowSpan: 1,
            colSpan: 1,
            hidden: false,
            fontFamily: 'Calibri',
            fontSize: 14,
            bold: false,
            italic: false,
            underline: false,
            textAlign: 'left',
            color: '#172033',
            background: '#ffffff',
            borderColor: '#d8e0ea',
            wrap: false
        };
    }

    private resetSelection(row: number, col: number): void {
        this.selectionStart = { row, col };
        this.selectionEnd = { row, col };
    }

    private forEachSelectedCell(callback: (cell: SheetCell, row: number, col: number) => void, affectedRows?: Set<number>): void {
        const range = this.selectedRange;
        for (let row = range.startRow; row <= range.endRow; row++) {
            for (let col = range.startCol; col <= range.endCol; col++) {
                const cell = this.sheetData[row]?.[col];
                if (cell && !cell.hidden) {
                    callback(cell, row, col);
                    affectedRows?.add(row);
                }
            }
        }
    }

    private recalculateWrappedRowHeights(rows?: Set<number>): void {
        const rowIndexes = rows ? Array.from(rows) : this.sheetData.map((_, row) => row);

        rowIndexes.forEach((rowIndex) => {
            const row = this.sheetData[rowIndex];
            if (!row) {
                return;
            }

            const neededHeight = row.reduce((height, cell, colIndex) => {
                if (cell.hidden || !cell.wrap || !cell.value) {
                    return height;
                }

                const width = (this.columnWidths[colIndex] || 130) * cell.colSpan;
                const usableWidth = Math.max(28, width - 20);
                const approxCharWidth = Math.max(6, cell.fontSize * 0.55);
                const charsPerLine = Math.max(1, Math.floor(usableWidth / approxCharWidth));
                const visualLines = cell.value.split(/\r?\n/).reduce((sum, line) => {
                    return sum + Math.max(1, Math.ceil(line.length / charsPerLine));
                }, 0);
                const lineHeight = cell.fontSize * 1.45;
                return Math.max(height, Math.ceil(visualLines * lineHeight + 16));
            }, 38);

            this.rowHeights[rowIndex] = this.clamp(neededHeight, 24, 240);
        });
    }

    private openContextMenu(event: MouseEvent, type: ContextMenuType): void {
        this.contextMenu = {
            visible: true,
            x: event.clientX,
            y: event.clientY,
            type
        };
    }

    private evaluateFormula(formula: string): string | number {
        const expression = formula.slice(1).trim();
        const aggregateMatch = expression.match(/^(SUM|AVG|MIN|MAX|COUNT)\(([A-Z]+\d+):([A-Z]+\d+)\)$/i);
        if (aggregateMatch) {
            const values = this.getRangeValues(aggregateMatch[2], aggregateMatch[3]);
            const numbers = values.map((value) => Number(value)).filter((value) => Number.isFinite(value));
            const fn = aggregateMatch[1].toUpperCase();
            if (fn === 'SUM') return numbers.reduce((sum, value) => sum + value, 0);
            if (fn === 'AVG') return numbers.length ? numbers.reduce((sum, value) => sum + value, 0) / numbers.length : 0;
            if (fn === 'MIN') return numbers.length ? Math.min(...numbers) : 0;
            if (fn === 'MAX') return numbers.length ? Math.max(...numbers) : 0;
            if (fn === 'COUNT') return numbers.length;
        }

        const replaced = expression.replace(/[A-Z]+\d+/gi, (ref) => {
            const cell = this.getCellByRef(ref);
            const value = cell ? this.getCellDisplayValue(cell) : 0;
            const numeric = Number(value);
            return Number.isFinite(numeric) ? String(numeric) : '0';
        });

        if (!/^[\d+\-*/().\s]+$/.test(replaced)) {
            return '#ERROR';
        }

        try {
            return Function(`"use strict"; return (${replaced});`)();
        } catch {
            return '#ERROR';
        }
    }

    private getRangeValues(startRef: string, endRef: string): string[] {
        const start = this.parseCellRef(startRef);
        const end = this.parseCellRef(endRef);
        if (!start || !end) {
            return [];
        }

        const values: string[] = [];
        for (let row = Math.min(start.row, end.row); row <= Math.max(start.row, end.row); row++) {
            for (let col = Math.min(start.col, end.col); col <= Math.max(start.col, end.col); col++) {
                values.push(this.sheetData[row]?.[col]?.value || '');
            }
        }
        return values;
    }

    private getCellByRef(ref: string): SheetCell | null {
        const parsed = this.parseCellRef(ref);
        if (!parsed) {
            return null;
        }
        return this.sheetData[parsed.row]?.[parsed.col] || null;
    }

    private parseCellRef(ref: string): { row: number; col: number } | null {
        const match = ref.toUpperCase().match(/^([A-Z]+)(\d+)$/);
        if (!match) {
            return null;
        }

        let col = 0;
        for (const char of match[1]) {
            col = col * 26 + (char.charCodeAt(0) - 64);
        }
        const row = Number(match[2]) - 1;
        return { row, col: col - 1 };
    }

    private createRow(): SheetCell[] {
        return Array.from({ length: this.sheetColumnCount }, () => this.createCell());
    }

    private unmergeAllCells(): void {
        this.sheetData.forEach((row) => {
            row.forEach((cell) => {
                cell.rowSpan = 1;
                cell.colSpan = 1;
                cell.hidden = false;
                cell.masterRow = undefined;
                cell.masterCol = undefined;
            });
        });
    }

    private unmergeRange(startRow: number, startCol: number, rowSpan: number, colSpan: number): void {
        for (let row = startRow; row < startRow + rowSpan && row < this.sheetRowCount; row++) {
            for (let col = startCol; col < startCol + colSpan && col < this.sheetColumnCount; col++) {
                const cell = this.sheetData[row][col];
                cell.rowSpan = 1;
                cell.colSpan = 1;
                cell.hidden = false;
                cell.masterRow = undefined;
                cell.masterCol = undefined;
            }
        }
    }

    private toColumnName(index: number): string {
        let name = '';
        let n = index + 1;
        while (n > 0) {
            const remainder = (n - 1) % 26;
            name = String.fromCharCode(65 + remainder) + name;
            n = Math.floor((n - remainder) / 26);
        }
        return name;
    }

    private clamp(value: number, min: number, max: number): number {
        return Math.max(min, Math.min(max, value));
    }
}
