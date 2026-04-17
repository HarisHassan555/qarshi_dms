declare module '@ckeditor/ckeditor5-watchdog/src/watchdog' {
  interface WatchdogConfig {
    crashNumberLimit?: number;
    minimumNonErrorTimePeriod?: number;
    [key: string]: unknown;
  }

  export default WatchdogConfig;
}
