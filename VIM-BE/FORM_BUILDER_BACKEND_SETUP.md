# Form Builder Backend Implementation

## Overview
Complete backend implementation for the Form Builder feature, allowing users to create and manage custom forms with dynamic fields.

## Database Setup

### Step 1: Create Database Tables
Run the SQL script to create the necessary tables:
```sql
-- Execute: VIM-BE/create_form_builder_tables.sql
```

This creates two tables:
- `cfg_tbl_custom_form` - Stores form definitions
- `cfg_tbl_custom_form_field` - Stores fields for each form

## Backend Components Created

### 1. Entities (JPA)
- **CfgTblCustomForm.java** - Entity for custom forms
  - Location: `VIM-BE/src/main/java/com/bezkoder/spring/login/sa/dal/entities/`
  - Fields: form name, description, status, timestamps, etc.
  - Relationship: One-to-Many with CfgTblCustomFormField

- **CfgTblCustomFormField.java** - Entity for form fields
  - Location: `VIM-BE/src/main/java/com/bezkoder/spring/login/sa/dal/entities/`
  - Fields: label, type, placeholder, required flag, order, etc.
  - Relationship: Many-to-One with CfgTblCustomForm

### 2. DAO Layer
- **ICfgTblCustomFormDAO.java** - DAO Interface
  - Location: `VIM-BE/src/main/java/com/bezkoder/spring/login/sa/dal/dao/`
  - Methods: getAllCustomForms, getCustomFormById, addNewCustomForm, updateCustomForm, deleteCustomForm, getActiveCustomForms

- **CfgTblCustomFormDAO.java** - DAO Implementation
  - Location: `VIM-BE/src/main/java/com/bezkoder/spring/login/sa/dal/daoimpl/`
  - Uses EntityManager for database operations
  - Handles eager loading of form fields
  - Implements soft delete

### 3. Service Layer
- **ICustomFormService.java** - Service Interface
  - Location: `VIM-BE/src/main/java/com/bezkoder/spring/login/sa/bll/services/`

- **CustomFormService.java** - Service Implementation
  - Location: `VIM-BE/src/main/java/com/bezkoder/spring/login/sa/bll/servicesimpl/`
  - Wraps DAO calls with logging

### 4. Controller Layer
- **CustomFormController.java** - REST Controller
  - Location: `VIM-BE/src/main/java/com/bezkoder/spring/login/controllers/`
  - Endpoints:
    - `GET /getAllCustomForms` - Get all forms
    - `GET /getCustomFormById?formId={id}` - Get form by ID
    - `POST /addNewCustomForm` - Create new form
    - `POST /updateCustomForm` - Update existing form
    - `POST /deleteCustomForm?formId={id}` - Delete form (soft delete)
    - `GET /getActiveCustomForms` - Get only active forms

## Frontend Integration

### Service
- **CustomFormService** - Angular service for API calls
  - Location: `VIM-FE/src/app/services/custom-form/custom-form.service.ts`
  - Methods: getAll(), getById(), save(), delete(), getActive()

### Component Updates
- **FormBuilderComponent** - Updated to use backend API
  - Replaced localStorage with backend service calls
  - Maps backend entity structure to frontend interface
  - Handles create, update, delete operations

## API Endpoints

### Create Form
```
POST /VIM/addNewCustomForm
Content-Type: application/json

{
  "txtFormName": "My Custom Form",
  "txtFormDescription": "Form description",
  "blIsActive": true,
  "blIsDeleted": false,
  "blnStatus": true,
  "cfgTblCustomFormFields": [
    {
      "txtFieldLabel": "First Name",
      "txtFieldType": "text",
      "txtPlaceholder": "Enter first name",
      "blIsRequired": true,
      "intFieldOrder": 0,
      "blIsActive": true,
      "blIsDeleted": false
    }
  ]
}
```

### Get All Forms
```
GET /VIM/getAllCustomForms
```

### Update Form
```
POST /VIM/updateCustomForm
Content-Type: application/json

{
  "serFormId": 1,
  "txtFormName": "Updated Form Name",
  ...
}
```

### Delete Form
```
POST /VIM/deleteCustomForm?formId=1
```

## Next Steps

1. **Run Database Script**: Execute `create_form_builder_tables.sql`
2. **Restart Backend**: Required for new entities to be recognized
3. **Test API Endpoints**: Use Postman or similar tool to test endpoints
4. **Frontend**: The frontend is already integrated and will work after backend restart

## Data Flow

1. User creates form in frontend → Frontend calls `CustomFormService.save()`
2. Service sends POST to `/addNewCustomForm`
3. Controller receives request → Calls `CustomFormService.addNewCustomForm()`
4. Service calls DAO → DAO persists to database
5. Response returned → Frontend updates UI

## Notes

- Forms use soft delete (blIsDeleted flag)
- Fields are ordered by `intFieldOrder`
- Cascade delete is configured for fields (when form is deleted)
- All timestamps and user tracking are automatically handled






