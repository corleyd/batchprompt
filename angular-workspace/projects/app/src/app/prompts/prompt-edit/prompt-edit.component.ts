import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators, FormArray } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { AuthService } from '@auth0/auth0-angular';
import { PromptService } from '../../services/prompt.service';
import { Prompt } from '../../models/prompt.model';
import { catchError, of, switchMap, take } from 'rxjs';

interface SchemaProperty {
  name: string;
  type: string;
  description: string;
}

@Component({
  selector: 'app-prompt-edit',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterModule],
  templateUrl: './prompt-edit.component.html',
  styleUrls: ['./prompt-edit.component.scss']
})
export class PromptEditComponent implements OnInit {
  promptForm: FormGroup;
  isEditMode = false;
  loading = false;
  saving = false;
  error: string | null = null;
  promptId: string | null = null;
  isAdvancedSchema = false;
  propertyTypes = ['string', 'number', 'boolean', 'array', 'integer'];
  outputFormatOptions = [
    { value: 'text_only', label: 'Text Only' },
    { value: 'schema', label: 'Structured Output' },
    { value: 'schema_with_text', label: 'Structured Output with Full Response Text' }
  ];

  constructor(
    private fb: FormBuilder,
    private promptService: PromptService,
    private route: ActivatedRoute,
    private router: Router,
    private auth: AuthService
  ) {
    this.promptForm = this.fb.group({
      name: ['', [Validators.required, Validators.maxLength(255)]],
      description: ['', [Validators.required, Validators.maxLength(255)]],
      promptText: ['', [Validators.required]],
      outputFormat: ['text_only', [Validators.required]],
      responseColumnName: ['response_text', [Validators.required, Validators.maxLength(255)]],
      includeFullResponse: [false],
      outputSchema: ['{}', []],
      schemaProperties: this.fb.array([])
    });

    // Update form validation based on outputFormat selection
    this.promptForm.get('outputFormat')?.valueChanges.subscribe(format => {
      this.updateFormValidation(format);
    });
  }

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      const promptId = params.get('id');
      if (promptId && promptId !== 'new') {
        this.promptId = promptId;
        this.isEditMode = true;
        this.loadPrompt(promptId);
      } else {
        // Add default empty property for new prompts
        this.addSchemaProperty();
      }
    });
  }

  updateFormValidation(format: string): void {
    const schemaControl = this.promptForm.get('outputSchema');
    const responseColumnNameControl = this.promptForm.get('responseColumnName');
    
    if (format === 'text_only') {
      schemaControl?.clearValidators();
      responseColumnNameControl?.setValidators([Validators.required, Validators.maxLength(255)]);
    } else {
      schemaControl?.setValidators([Validators.required]);
      if (format === 'schema_with_text') {
        responseColumnNameControl?.setValidators([Validators.required, Validators.maxLength(255)]);
      } else {
        responseColumnNameControl?.clearValidators();
      }
    }
    
    schemaControl?.updateValueAndValidity();
    responseColumnNameControl?.updateValueAndValidity();
  }

  get schemaProperties(): FormArray {
    return this.promptForm.get('schemaProperties') as FormArray;
  }

  addSchemaProperty(): void {
    this.schemaProperties.push(this.fb.group({
      name: ['', Validators.required],
      type: ['string', Validators.required],
      description: ['', Validators.required]
    }));
    this.updateOutputSchemaFromProperties();
  }

  removeSchemaProperty(index: number): void {
    this.schemaProperties.removeAt(index);
    this.updateOutputSchemaFromProperties();
  }

  toggleSchemaMode(): void {
    this.isAdvancedSchema = !this.isAdvancedSchema;
    
    if (this.isAdvancedSchema) {
      this.updateOutputSchemaFromProperties();
    } else {
      try {
        this.convertJsonSchemaToProperties();
      } catch (e) {
        // If conversion fails, revert to advanced mode
        this.isAdvancedSchema = true;
        this.error = 'Cannot convert to simple structured output format. Using advanced mode.';
      }
    }
  }

  isSimpleSchema(schemaStr: string): boolean {
    try {
      const schema = JSON.parse(schemaStr);
      
      // Check if it's a single object with properties
      if (!schema.type || schema.type !== 'object' || !schema.properties) {
        return false;
      }
      
      // Check if all properties are required
      const requiredProps = schema.required || [];
      const propertyNames = Object.keys(schema.properties);
      
      if (propertyNames.length === 0 || !requiredProps.length || 
          !propertyNames.every(prop => requiredProps.includes(prop))) {
        return false;
      }
      
      // Check if all properties are simple types
      return propertyNames.every(propName => {
        const prop = schema.properties[propName];
        return prop && prop.type && 
               ['string', 'number', 'boolean', 'integer', 'array'].includes(prop.type) &&
               !prop.properties && !prop.items?.properties;
      });
    } catch (e) {
      return false;
    }
  }

  convertJsonSchemaToProperties(): void {
    const schemaStr = this.promptForm.get('outputSchema')?.value;
    if (!schemaStr || !this.validateJson(schemaStr)) {
      return;
    }

    try {
      const schema = JSON.parse(schemaStr);
      
      if (!schema.properties) {
        throw new Error('No properties found in schema');
      }
      
      this.schemaProperties.clear();
      
      Object.entries(schema.properties).forEach(([name, propDetails]: [string, any]) => {
        this.schemaProperties.push(this.fb.group({
          name: [name, Validators.required],
          type: [propDetails.type || 'string', Validators.required],
          description: [propDetails.description || '', Validators.required]
        }));
      });
    } catch (e) {
      throw new Error('Unable to convert JSON schema to simple properties');
    }
  }

  updateOutputSchemaFromProperties(): void {
    if (this.schemaProperties.length === 0) {
      return;
    }

    const properties: Record<string, any> = {};
    const required: string[] = [];
    
    this.schemaProperties.controls.forEach(control => {
      const prop = control.value;
      if (prop.name) {
        properties[prop.name] = {
          type: prop.type,
          description: prop.description
        };
        required.push(prop.name);
      }
    });
    
    const schema = {
      type: 'object',
      properties: properties,
      required: required
    };
    
    this.promptForm.get('outputSchema')?.setValue(JSON.stringify(schema, null, 2));
  }

  loadPrompt(promptId: string): void {
    this.loading = true;
    this.promptService.getPromptById(promptId)
      .subscribe({
        next: (prompt) => {
          this.promptForm.patchValue({
            name: prompt.name,
            description: prompt.description,
            promptText: prompt.promptText,
            outputSchema: prompt.outputSchema,
            outputFormat: prompt.outputFormat || 'schema',
            responseColumnName: prompt.responseColumnName || 'response_text',
            includeFullResponse: prompt.includeFullResponse || false
          });
          
          // Determine if it's a simple schema
          this.isAdvancedSchema = !this.isSimpleSchema(prompt.outputSchema);
          
          if (!this.isAdvancedSchema) {
            try {
              this.convertJsonSchemaToProperties();
            } catch (e) {
              this.isAdvancedSchema = true;
            }
          }
          
          this.loading = false;
        },
        error: (error) => {
          console.error('Error loading prompt', error);
          this.error = 'Failed to load prompt details. Please try again.';
          this.loading = false;
        }
      });
  }

  onSubmit(): void {
    if (this.promptForm.invalid) {
      this.markFormGroupTouched(this.promptForm);
      return;
    }

    // Make sure output schema is updated from properties if in simple mode
    if (!this.isAdvancedSchema && this.promptForm.get('outputFormat')?.value !== 'text_only') {
      this.updateOutputSchemaFromProperties();
    }

    this.saving = true;
    const outputFormat = this.promptForm.get('outputFormat')?.value;
    
    const promptData: Prompt = {
      name: this.promptForm.get('name')?.value,
      description: this.promptForm.get('description')?.value,
      promptText: this.promptForm.get('promptText')?.value,
      outputFormat: outputFormat,
      responseColumnName: outputFormat === 'text_only' || outputFormat === 'schema_with_text' 
        ? this.promptForm.get('responseColumnName')?.value 
        : undefined,
      includeFullResponse: outputFormat === 'schema_with_text' 
        ? true 
        : (outputFormat === 'text_only' ? false : this.promptForm.get('includeFullResponse')?.value),
      outputSchema: outputFormat === 'text_only' 
        ? '{}' 
        : this.promptForm.get('outputSchema')?.value,
      userId: ''
    };
    
    this.auth.user$
      .pipe(
        take(1),
        switchMap(user => {
          if (!user?.sub) {
            throw new Error('User not authenticated');
          }
          
          promptData.userId = user.sub;
          
          if (this.isEditMode && this.promptId) {
            return this.promptService.updatePrompt(this.promptId, promptData);
          } else {
            return this.promptService.createPrompt(promptData);
          }
        }),
        catchError(error => {
          console.error('Error saving prompt', error);
          this.error = 'Failed to save prompt. Please try again.';
          this.saving = false;
          return of(null);
        })
      )
      .subscribe({
        next: (response) => {
          if (response) {
            this.saving = false;
            this.router.navigate(['/prompts']);
          }
        }
      });
  }

  markFormGroupTouched(formGroup: FormGroup) {
    Object.values(formGroup.controls).forEach(control => {
      control.markAsTouched();
      if ((control as any).controls) {
        this.markFormGroupTouched(control as FormGroup);
      }
    });
  }

  cancel(): void {
    this.router.navigate(['/prompts']);
  }

  // Helper method to validate if JSON is valid
  validateJson(json: string): boolean {
    try {
      JSON.parse(json);
      return true;
    } catch (e) {
      return false;
    }
  }

  // Method to format the JSON schema
  formatJsonSchema(): void {
    try {
      const value = this.promptForm.get('outputSchema')?.value;
      if (value && this.validateJson(value)) {
        const formatted = JSON.stringify(JSON.parse(value), null, 2);
        this.promptForm.get('outputSchema')?.setValue(formatted);
      }
    } catch (e) {
      // Just ignore if can't format
    }
  }
}