import { Component } from '@angular/core';

@Component({
  selector: 'app-settings',
  templateUrl: './settings.component.html',
  styleUrls: ['./settings.component.scss']
})
export class SettingsComponent {
  // Sample settings categories and their options
  settingsCategories = [
    {
      name: 'General',
      icon: 'settings',
      settings: [
        { name: 'Site Name', value: 'BatchPrompt', type: 'text' },
        { name: 'Contact Email', value: 'admin@batchprompt.ai', type: 'email' },
        { name: 'Max Upload Size (MB)', value: '100', type: 'number' }
      ]
    },
    {
      name: 'Security',
      icon: 'shield',
      settings: [
        { name: 'Two-Factor Authentication', value: true, type: 'toggle' },
        { name: 'Session Timeout (minutes)', value: '30', type: 'number' },
        { name: 'Password Expiry (days)', value: '90', type: 'number' }
      ]
    },
    {
      name: 'API',
      icon: 'code',
      settings: [
        { name: 'API Rate Limit', value: '1000', type: 'number' },
        { name: 'API Token Expiry (hours)', value: '24', type: 'number' },
        { name: 'Debug Mode', value: false, type: 'toggle' }
      ]
    }
  ];

  // Currently selected settings category
  currentCategory = this.settingsCategories[0];

  selectCategory(category: any) {
    this.currentCategory = category;
  }

  saveSettings() {
    // Placeholder for saving settings functionality
    console.log('Settings saved!', this.settingsCategories);
    // In a real app, this would call a service to save settings to the backend
  }
}