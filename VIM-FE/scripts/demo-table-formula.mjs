import { chromium } from 'playwright';

const BASE = 'http://localhost:4200/velocity';
const LOGIN = 'admin';
const PASSWORD = 'password';

async function dismissChrome(page) {
  await page.keyboard.press('Escape').catch(() => {});
  await page.evaluate(() => {
    const hide = (el) => {
      if (!el) return;
      el.style.setProperty('display', 'none', 'important');
      el.style.setProperty('pointer-events', 'none', 'important');
    };
    document.querySelectorAll(
      '.customizer, .theme-customizer, .offcanvas, .introjs-overlay, .introjs-helperLayer, .driver-overlay, .shepherd-modal-overlay-container, .onboarding, .tour-overlay'
    ).forEach(hide);
    document.body.classList.remove('modal-open', 'overflow-hidden');
    document.documentElement.style.pointerEvents = 'auto';
    document.body.style.pointerEvents = 'auto';
  });
}

const browser = await chromium.launch({
  headless: false,
  slowMo: 400,
  args: ['--start-maximized'],
});
const context = await browser.newContext({ viewport: null });
const page = await context.newPage();

try {
  await page.goto(`${BASE}/auth/signin?returnUrl=%2Ftemplate-builder`, { waitUntil: 'domcontentloaded' });

  if (page.url().includes('/auth/signin')) {
    await page.getByPlaceholder('Enter Login Name').fill(LOGIN);
    await page.getByPlaceholder('Enter Password').fill(PASSWORD);
    await page.getByRole('button', { name: 'Sign In' }).click();
  }

  await page.waitForURL('**/template-builder', { timeout: 30000 });
  await page.waitForSelector('.template-ckeditor .ck-editor__editable', { timeout: 30000 });
  await page.waitForTimeout(1500);
  await dismissChrome(page);

  const tableReady = await page.evaluate(() => {
    const editable = document.querySelector('.template-ckeditor .ck-editor__editable');
    const editor = editable && editable.ckeditorInstance;
    if (!editor) return false;
    editor.setData(`
      <figure class="table">
        <table>
          <tbody>
            <tr><td>Qty</td><td>Price</td><td>Total</td></tr>
            <tr><td>10</td><td>25</td><td></td></tr>
          </tbody>
        </table>
      </figure>
      <h2 style="text-align:center">Template Title</h2>
      <p>Watch the Total cell: 10 + 25.</p>
    `);
    const table = document.querySelector('.ck-content table');
    if (table) table.scrollIntoView({ block: 'center' });
    return true;
  });
  if (!tableReady) throw new Error('CKEditor was not ready');
  await page.waitForTimeout(800);

  await page.getByRole('button', { name: 'Table formula' }).click({ force: true });
  await page.getByRole('heading', { name: 'Table formula (easy mode)' }).waitFor({ timeout: 10000 });
  await page.waitForTimeout(400);

  await page.locator('.table-formula-picker-cell', { hasText: 'Empty' }).click({ force: true });
  await page.getByRole('radio', { name: 'Pick sources' }).click({ force: true });
  await page.locator('.table-formula-picker-cell', { hasText: '10' }).click({ force: true });
  await page.locator('.table-formula-picker-cell', { hasText: '25' }).click({ force: true });
  await page.getByRole('button', { name: 'Apply formula' }).click({ force: true });

  await page.waitForTimeout(1200);
  await page.locator('.template-formula-value').first().waitFor({ timeout: 8000 }).catch(() => {});
  const label = await page.locator('.template-formula-value').first().innerText().catch(() => '');
  console.log('Formula result:', label || '(applied — check the Total cell)');
  console.log('Browser stays open 3 minutes so you can look. Close the window when done.');
  await page.waitForTimeout(180000);
} catch (error) {
  console.error('Demo failed:', error.message);
  await page.screenshot({ path: 'scripts/demo-table-formula-error.png', fullPage: true }).catch(() => {});
  await page.waitForTimeout(60000);
} finally {
  await browser.close();
}
