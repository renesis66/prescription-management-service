import Joi from 'joi';

export const createPrescriptionSchema = Joi.object({
  medicationName: Joi.string().min(1).max(100).required(),
  dosage: Joi.number().positive().required(),
  unit: Joi.string().valid('mg', 'g', 'ml', 'tablets', 'capsules').required(),
  frequencyHours: Joi.number().integer().min(1).max(24).required(),
  startTime: Joi.string().pattern(/^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$/).required(),
  startDate: Joi.string().isoDate().required(),
  endDate: Joi.string().isoDate().required(),
  prescribedBy: Joi.string().min(1).max(100).required()
}).custom((value, helpers) => {
  const startDate = new Date(value.startDate);
  const endDate = new Date(value.endDate);
  
  if (endDate <= startDate) {
    return helpers.error('any.invalid', { message: 'End date must be after start date' });
  }
  
  if (startDate < new Date(new Date().toDateString())) {
    return helpers.error('any.invalid', { message: 'Start date cannot be in the past' });
  }
  
  return value;
});

export const updatePrescriptionSchema = Joi.object({
  medicationName: Joi.string().min(1).max(100),
  dosage: Joi.number().positive(),
  unit: Joi.string().valid('mg', 'g', 'ml', 'tablets', 'capsules'),
  frequencyHours: Joi.number().integer().min(1).max(24),
  startTime: Joi.string().pattern(/^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$/),
  startDate: Joi.string().isoDate(),
  endDate: Joi.string().isoDate(),
  status: Joi.string().valid('ACTIVE', 'COMPLETED', 'CANCELLED')
}).min(1);

export const patientIdSchema = Joi.string().uuid().required();
export const prescriptionIdSchema = Joi.string().uuid().required();

export const validateRequest = (schema: Joi.Schema, data: any) => {
  const { error, value } = schema.validate(data);
  if (error) {
    throw new Error(`Validation error: ${error.details[0].message}`);
  }
  return value;
};