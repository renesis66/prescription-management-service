import { Router } from 'express';
import { PrescriptionController } from '../controllers/PrescriptionController';

const router = Router();
const prescriptionController = new PrescriptionController();

router.get('/patients/:patientId/prescriptions', (req, res) => 
  prescriptionController.getPatientPrescriptions(req, res)
);

router.post('/patients/:patientId/prescriptions', (req, res) => 
  prescriptionController.createPrescription(req, res)
);

router.put('/prescriptions/:id', (req, res) => 
  prescriptionController.updatePrescription(req, res)
);

router.delete('/prescriptions/:id', (req, res) => 
  prescriptionController.deletePrescription(req, res)
);

router.get('/prescriptions/:id/schedule', (req, res) => 
  prescriptionController.getPrescriptionSchedule(req, res)
);

export default router;