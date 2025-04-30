import { ComponentFixture, TestBed } from '@angular/core/testing';

import { FileStatusPageComponent } from './file-status-page.component';

describe('FileStatusPageComponent', () => {
  let component: FileStatusPageComponent;
  let fixture: ComponentFixture<FileStatusPageComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FileStatusPageComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(FileStatusPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
