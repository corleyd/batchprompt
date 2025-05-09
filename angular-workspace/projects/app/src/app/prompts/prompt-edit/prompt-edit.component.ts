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
  outputMethodOptions = [
    { value: 'TEXT', label: 'Text Only' },
    { value: 'STRUCTURED', label: 'Structured Output' },
    { value: 'BOTH', label: 'Structured Output with Full Response Text' }
  ];

  // Flag to track whether we're in the process of loading a prompt
  private isLoadingExistingPrompt = false;

  constructor(
    private fb: FormBuilder,
    private promptService: PromptService,
    private route: ActivatedRoute,
    private router: Router,
    private auth: AuthService
  ) {
    this.promptForm = this.fb.group({
      name: ['', [Validators.required]],
      description: ['', [Validators.required]],
      promptText: ['', [Validators.required]],
      outputMethod: ['TEXT', [Validators.required]],
      responseColumnName: ['response_text', [Validators.required]],
      outputSchema: ['{}', []],
      schemaProperties: this.fb.array([])
    });

    // Update form validation based on outputMethod selection
    this.promptForm.get('outputMethod')?.valueChanges.subscribe(format => {
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
    console.log("updateFormValidation", format, schemaControl);
    console.log("Current schema value:", this.promptForm.get('outputSchema')?.value);
    console.log("Current isAdvancedSchema:", this.isAdvancedSchema);
    console.log("isLoadingExistingPrompt:", this.isLoadingExistingPrompt);
    const responseColumnNameControl = this.promptForm.get('responseColumnName');
    
    // Handle schema controls
    if (format === 'TEXT') {
      schemaControl?.clearValidators();
      responseColumnNameControl?.setValidators([Validators.required, Validators.maxLength(255)]);
      
      // Handle schema properties individually for TEXT mode
      for (let i = 0; i < this.schemaProperties.length; i++) {
        const propertyGroup = this.schemaProperties.at(i) as FormGroup;
        propertyGroup.get('name')?.clearValidators();
        propertyGroup.get('type')?.clearValidators();
        propertyGroup.get('description')?.clearValidators();
        
        propertyGroup.get('name')?.updateValueAndValidity({emitEvent: false});
        propertyGroup.get('type')?.updateValueAndValidity({emitEvent: false});
        propertyGroup.get('description')?.updateValueAndValidity({emitEvent: false});
      }
    } else {
      schemaControl?.setValidators([Validators.required]);
      
      // Ensure at least one schema property for non-TEXT modes
      // But don't add a property if we're loading a prompt (handled in loadPrompt)
      if (this.schemaProperties.length === 0 && !this.isLoadingExistingPrompt) {
        this.addSchemaProperty();
      }
      
      // Apply proper validators for non-TEXT modes
      for (let i = 0; i < this.schemaProperties.length; i++) {
        const propertyGroup = this.schemaProperties.at(i) as FormGroup;
        propertyGroup.get('name')?.setValidators([Validators.required]);
        propertyGroup.get('type')?.setValidators([Validators.required]);
        propertyGroup.get('description')?.setValidators([Validators.required]);
        
        propertyGroup.get('name')?.updateValueAndValidity({emitEvent: false});
        propertyGroup.get('type')?.updateValueAndValidity({emitEvent: false});
        propertyGroup.get('description')?.updateValueAndValidity({emitEvent: false});
      }
      
      if (format === 'BOTH') {
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
    console.log("addSchemaProperty - before adding property");
    console.log("Current schema:", this.promptForm.get('outputSchema')?.value);
    console.log("Current isAdvancedSchema:", this.isAdvancedSchema);
    
    this.schemaProperties.push(this.fb.group({
      name: ['', Validators.required],
      type: ['string', Validators.required],
      description: ['', Validators.required]
    }));
    
    // Only update the schema if we're not loading an existing prompt
    // or if we are in simple mode
    if (!this.loading || !this.isAdvancedSchema) {
      console.log("Calling updateOutputSchemaFromProperties from addSchemaProperty");
      this.updateOutputSchemaFromProperties();
    } else {
      console.log("Skipping updateOutputSchemaFromProperties during loading");
    }
    
    console.log("addSchemaProperty - after adding property");
    console.log("Updated schema:", this.promptForm.get('outputSchema')?.value);
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
    console.log("isSimpleSchema", schemaStr);
    try {
      const schema = JSON.parse(schemaStr);
      
      // Check if it's a single object with properties
      if (!schema.type || schema.type !== 'object' || !schema.properties) {
        console.log("Not a simple schema: not an object or no properties");
        return false;
      }
      
      // Check if all properties are required
      const requiredProps = schema.required || [];
      const propertyNames = Object.keys(schema.properties);
      
      if (propertyNames.length === 0 || !requiredProps.length || 
          !propertyNames.every(prop => requiredProps.includes(prop))) {
        console.log("Not a simple schema: properties are not all required");
        return false;
      }
      
      // Check if all properties are simple types
      let value = propertyNames.every(propName => {
        const prop = schema.properties[propName];
        let propResult = prop && prop.type && 
               ['string', 'number', 'boolean', 'integer', 'array'].includes(prop.type) &&
               !prop.properties && !prop.items?.properties;
        if (!propResult) {
          console.log("Not a simple schema: property type is not simple: ", prop);
        }
        return propResult;
      });

      if (!value) {
        console.log("Not a simple schema: properties are not simple types");
      } else {
        console.log("Simple schema: all properties are simple types");
      }
      return value;

    } catch (e) {
      console.log("exception while parsing schema", e);
      return false;
    }
  }

  convertJsonSchemaToProperties(): void {
    const schemaStr = this.promptForm.get('outputSchema')?.value;
    console.log("schemaStr", schemaStr);
    if (!schemaStr || !this.validateJson(schemaStr)) {
      return;
    }

    try {
      const schema = JSON.parse(schemaStr);

      console.log("schema", schema);
      
      if (!schema.properties) {
        throw new Error('No properties found in schema');
      }
      
      this.schemaProperties.clear();

      console.log("schema.properties", schema.properties);
      
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
    console.log("updateOutputSchemaFromProperties", this.schemaProperties, this.schemaProperties.length);

    if (this.schemaProperties.length === 0) {
      console.log("No schema properties to update");
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
    this.isLoadingExistingPrompt = true;
    this.promptService.getPromptById(promptId)
      .subscribe({
        next: (prompt) => {
          console.log("PROMPT", prompt);
          console.log("Loading prompt with schema:", prompt.responseJsonSchema);
          console.log("Output method:", prompt.outputMethod);
          
          // Store the original schema before form patching
          const originalSchema = prompt.responseJsonSchema;
          
          this.promptForm.patchValue({
            name: prompt.name,
            description: prompt.description,
            promptText: prompt.promptText,
            // Hold off on setting the schema until we've determined the mode
            outputMethod: prompt.outputMethod || 'TEXT',
            responseColumnName: prompt.responseTextColumnName || 'response_text',
          });
          
          // Handle schema mode determination for non-TEXT output methods
          if (prompt.outputMethod === 'STRUCTURED' || prompt.outputMethod === 'BOTH') {
            // Determine if it's a simple schema
            const isSimple = this.isSimpleSchema(originalSchema);
            console.log("Schema is simple:", isSimple);
            this.isAdvancedSchema = !isSimple;
            console.log("Setting isAdvancedSchema to:", this.isAdvancedSchema);
            
            // Now set the schema AFTER determining the mode
            this.promptForm.get('outputSchema')?.setValue(originalSchema);
            
            if (!this.isAdvancedSchema) {
              try {
                console.log("Converting JSON schema to properties");
                // Clear existing schema properties before conversion
                while (this.schemaProperties.length) {
                  this.schemaProperties.removeAt(0);
                }
                this.convertJsonSchemaToProperties();
                console.log("Schema properties after conversion:", this.schemaProperties.value);
              } catch (e) {
                console.error("Error converting schema:", e);
                this.isAdvancedSchema = true;
                console.log("Falling back to advanced mode due to error");
              }
            }
            console.log("Final schema after loading:", this.promptForm.get('outputSchema')?.value);
          } else {
            // For TEXT mode, add a default property
            this.addSchemaProperty();
          }
          
          this.loading = false;
          this.isLoadingExistingPrompt = false;
        },
        error: (error) => {
          console.error('Error loading prompt', error);
          this.error = 'Failed to load prompt details. Please try again.';
          this.loading = false;
          this.isLoadingExistingPrompt = false;
        }
      });
  }

  onSubmit(): void {
    // Get current output format
    const outputMethod = this.promptForm.get('outputMethod')?.value;
    
    // For TEXT mode, manually validate only the relevant controls
    if (outputMethod === 'TEXT') {
      const relevantControls = ['name', 'description', 'promptText', 'responseColumnName'];
      const isValid = relevantControls.every(controlName => {
        const control = this.promptForm.get(controlName);
        return control && control.valid;
      });
      
      // If any of the relevant controls are invalid, mark them as touched and return
      if (!isValid) {
        relevantControls.forEach(controlName => {
          const control = this.promptForm.get(controlName);
          if (control) {
            control.markAsTouched();
          }
        });
        return;
      }
    } else if (this.promptForm.invalid) {
      // For other formats, check the entire form
      this.markFormGroupTouched(this.promptForm);
      return;
    }

    // Make sure output schema is updated from properties if in simple mode
    if (!this.isAdvancedSchema && outputMethod !== 'TEXT') {
      this.updateOutputSchemaFromProperties();
    }

    this.saving = true;
    
    const promptData: Prompt = {
      name: this.promptForm.get('name')?.value,
      description: this.promptForm.get('description')?.value,
      promptText: this.promptForm.get('promptText')?.value,
      outputMethod: outputMethod,
      responseTextColumnName: outputMethod === 'TEXT' || outputMethod === 'BOTH' 
        ? this.promptForm.get('responseColumnName')?.value 
        : undefined,
      responseJsonSchema: outputMethod === 'TEXT' 
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
            this.router.navigate(['/dashboard/prompts']);
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
    this.router.navigate(['/dashboard/prompts']);
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