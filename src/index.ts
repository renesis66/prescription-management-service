import express from 'express';
import cors from 'cors';
import helmet from 'helmet';
import morgan from 'morgan';
import AWS from 'aws-sdk';
import prescriptionRoutes from './routes/prescriptions';

const app = express();
const port = process.env.PORT || 3000;

AWS.config.update({
  region: process.env.AWS_REGION || 'us-east-1'
});

app.use(helmet());
app.use(cors());
app.use(morgan('combined'));
app.use(express.json());

app.use('/api', prescriptionRoutes);

app.get('/health', (req, res) => {
  res.json({ 
    status: 'healthy', 
    service: 'prescription-management-service',
    timestamp: new Date().toISOString()
  });
});

app.use((req, res) => {
  res.status(404).json({
    success: false,
    error: 'Endpoint not found'
  });
});

app.use((error: Error, req: express.Request, res: express.Response, next: express.NextFunction) => {
  console.error('Unhandled error:', error);
  res.status(500).json({
    success: false,
    error: 'Internal server error'
  });
});

app.listen(port, () => {
  console.log(`Prescription Management Service running on port ${port}`);
});

export default app;