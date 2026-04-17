import { Component, Input } from '@angular/core';
import { EditorMetrics } from '../../models/document.model';

@Component({
  selector: 'app-word-statusbar',
  templateUrl: './word-statusbar.component.html',
  styleUrls: ['./word-statusbar.component.css']
})
export class WordStatusbarComponent {
  @Input() metrics: EditorMetrics = { words: 0, characters: 0 };
  @Input() zoom = 100;
  @Input() savedAt = '';
}

